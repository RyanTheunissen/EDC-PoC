import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import { CatalogService, CatalogRequest } from '../services/catalog.service';

interface ProviderEndpoint {
  label: string;                // display name in UI
  counterPartyAddress: string;  // /protocol endpoint
  protocol: string;
}

interface AssetCard {
  id: string;
  offerId: string;
  formats: string[];
  rawDataset: any;

  // federated info
  providerLabel: string;
  providerAddress: string;
  protocol: string;
  participantId: string;
}

@Component({
  selector: 'app-catalog',
  standalone: true,
  templateUrl: './catalog.html',
  styleUrls: ['./catalog.css'],
  imports: [
    CommonModule,
    FormsModule,
    RouterModule
  ]
})
export class Catalog {
  counterPartyAddress = 'http://provider:19194/protocol';
  protocol = 'dataspace-protocol-http';

  providerEndpoints: ProviderEndpoint[] = [
    {
      label: 'bodegraven',
      counterPartyAddress: 'http://100.92.114.63:19194/protocol',
      protocol: 'dataspace-protocol-http'
    },
    {
      label: 'dataspace1',
      counterPartyAddress: 'http://100.101.111.95:19194/protocol',
      protocol: 'dataspace-protocol-http'
    }
  ];

  loading = false;
  error: string | null = null;

  assets: AssetCard[] = [];
  selectedAsset: AssetCard | null = null;
  rawCatalog: any = null;

  participantId: string | null = null;

  negotiationJsonText = '';
  negotiationResponse: any = null;
  negotiationId: string | null = null;
  negotiationLoading = false;
  negotiationError: string | null = null;

  negotiationStatus: any = null;
  negotiationStatusLoading = false;
  negotiationStatusError: string | null = null;

  transferJsonText = '';
  transferResponse: any = null;
  transferLoading = false;
  transferError: string | null = null;

  constructor(private catalogService: CatalogService) {}

  // === Single-provider catalog fetch (uses the form fields) ===
  onFetchCatalog() {
    this.loading = true;
    this.error = null;
    this.assets = [];
    this.selectedAsset = null;
    this.rawCatalog = null;
    this.participantId = null;

    const request: CatalogRequest = {
      '@context': {
        '@vocab': 'https://w3id.org/edc/v0.0.1/ns/'
      },
      counterPartyAddress: this.counterPartyAddress,
      protocol: this.protocol
    };

    // provider descriptor based on current form values
    const provider: ProviderEndpoint = {
      label: 'Handmatig ingevoerd',
      counterPartyAddress: this.counterPartyAddress,
      protocol: this.protocol
    };

    this.catalogService.fetchCatalog(request).subscribe({
      next: (response) => {
        this.rawCatalog = response;
        this.participantId = response['dspace:participantId'] || 'provider';
        this.assets = this.mapToAssets(response, provider, this.participantId ?? 'unknown participant');
        this.loading = false;
      },
      error: (err) => {
        console.error('Catalog error', err);
        this.error = 'Kon catalogus niet ophalen (zie console voor details).';
        this.loading = false;
      }
    });
  }

  onFetchFederatedCatalog() {
    this.loading = true;
    this.error = null;
    this.assets = [];
    this.selectedAsset = null;
    this.rawCatalog = null;
    this.participantId = null;

    if (this.providerEndpoints.length === 0) {
      this.loading = false;
      this.error = 'Geen provider endpoints geconfigureerd.';
      return;
    }

    const requests = this.providerEndpoints.map((p) => {
      const req: CatalogRequest = {
        '@context': {
          '@vocab': 'https://w3id.org/edc/v0.0.1/ns/'
        },
        counterPartyAddress: p.counterPartyAddress,
        protocol: p.protocol
      };
      return this.catalogService.fetchCatalog(req);
    });

    forkJoin(requests).subscribe({
      next: (responses) => {
        const allAssets: AssetCard[] = [];

        responses.forEach((resp, idx) => {
          const provider = this.providerEndpoints[idx];
          const participantId = resp['dspace:participantId'] || provider.label;
          const mapped = this.mapToAssets(resp, provider, participantId);
          allAssets.push(...mapped);
        });

        this.assets = allAssets;
        this.rawCatalog = responses; // array of raw catalog responses
        this.loading = false;
      },
      error: (err) => {
        console.error('Federated catalog error', err);
        this.error = 'Federated catalog ophalen faalde (zie console).';
        this.loading = false;
      }
    });
  }

  onSelectAsset(asset: AssetCard) {
    this.selectedAsset = asset;
    this.negotiationResponse = null;
    this.negotiationId = null;
    this.negotiationStatus = null;
    this.transferResponse = null;
    this.transferJsonText = '';

    this.counterPartyAddress = asset.providerAddress;
    this.protocol = asset.protocol;
    this.participantId = asset.participantId;

    const rawPolicy =
      asset.rawDataset['odrl:hasPolicy'] ||
      asset.rawDataset['odrl:policy'] ||
      {};
    const policyFromCatalog = Array.isArray(rawPolicy)
      ? rawPolicy[0]
      : rawPolicy || {};

    const policyClone = Object.keys(policyFromCatalog).length
      ? JSON.parse(JSON.stringify(policyFromCatalog))
      : {};

    if (!('@context' in policyClone)) {
      policyClone['@context'] = 'http://www.w3.org/ns/odrl.jsonld';
    }
    if (!('@type' in policyClone)) {
      policyClone['@type'] = 'Offer';
    }
    if (!('assigner' in policyClone)) {
      policyClone['assigner'] = this.participantId || 'provider';
    }
    if (!('target' in policyClone)) {
      policyClone['target'] = asset.id;
    }

    const contractRequestBody = {
      '@context': {
        '@vocab': 'https://w3id.org/edc/v0.0.1/ns/'
      },
      '@type': 'ContractRequest',
      counterPartyAddress: this.counterPartyAddress,
      protocol: this.protocol,
      policy: policyClone
    };

    this.negotiationJsonText = JSON.stringify(contractRequestBody, null, 2);
  }

  private mapToAssets(
    catalog: any,
    provider: ProviderEndpoint,
    participantId: string
  ): AssetCard[] {
    if (!catalog) return [];

    const datasets = catalog['dcat:dataset'];
    if (!datasets) return [];

    const dsArray = Array.isArray(datasets) ? datasets : [datasets];

    return dsArray.map((ds: any, index: number) => {
      const id = ds.id || ds['@id'] || `dataset-${index}`;

      const offer = ds['odrl:hasPolicy'] || {};
      const offerId = offer['@id'] || '';

      const distributions = ds['dcat:distribution'] || [];
      const distArray = Array.isArray(distributions)
        ? distributions
        : [distributions];

      const formats = distArray
        .map((d: any) => d?.['dct:format']?.['@id'])
        .filter((f: any) => !!f);

      return {
        id,
        offerId,
        formats,
        rawDataset: ds,
        providerLabel: provider.label,
        providerAddress: provider.counterPartyAddress,
        protocol: provider.protocol,
        participantId
      } as AssetCard;
    });
  }

  onSendNegotiation() {
    if (!this.negotiationJsonText.trim()) {
      this.negotiationError = 'Negotiation JSON is leeg.';
      return;
    }

    let body: any;
    try {
      body = JSON.parse(this.negotiationJsonText);
    } catch (e) {
      this.negotiationError = 'Negotiation JSON is geen geldige JSON.';
      return;
    }

    this.negotiationLoading = true;
    this.negotiationError = null;
    this.negotiationResponse = null;

    this.catalogService.negotiateContract(body).subscribe({
      next: (resp) => {
        this.negotiationResponse = resp;
        this.negotiationLoading = false;

        const id = resp?.id || resp?.['@id'];
        if (id) {
          this.negotiationId = id;
        }
      },
      error: (err) => {
        console.error('Negotiation error', err);
        this.negotiationError =
          'Negotiation request faalde (zie console voor details).';
        this.negotiationLoading = false;
      }
    });
  }

  onRefreshNegotiationStatus() {
    if (!this.negotiationId) {
      this.negotiationStatusError = 'Geen negotiation ID beschikbaar.';
      return;
    }

    this.negotiationStatusLoading = true;
    this.negotiationStatusError = null;

    this.catalogService.getNegotiation(this.negotiationId).subscribe({
      next: (resp) => {
        this.negotiationStatus = resp;
        this.negotiationStatusLoading = false;

        const agreementId = resp?.contractAgreementId;
        if (agreementId && !this.transferJsonText) {
          const connectorId = this.participantId || 'provider';

          const transferBody = {
            '@context': {
              '@vocab': 'https://w3id.org/edc/v0.0.1/ns/'
            },
            '@type': 'TransferRequestDto',
            connectorId: connectorId,
            counterPartyAddress: this.counterPartyAddress,
            contractId: agreementId,
            protocol: this.protocol,
            transferType: 'AmazonS3-PUSH',
            dataDestination: {
              type: 'AmazonS3',
              region: 'us-east-1',
              bucketName: 'src-bucket',
              objectName: 'test.csv',
              endpointOverride: 'http://minio:9000'
            }
          };

          this.transferJsonText = JSON.stringify(transferBody, null, 2);
        }
      },
      error: (err) => {
        console.error('Negotiation status error', err);
        this.negotiationStatusError =
          'Kon negotiation status niet ophalen (zie console).';
        this.negotiationStatusLoading = false;
      }
    });
  }

  onStartTransfer() {
    if (!this.transferJsonText.trim()) {
      this.transferError = 'Transfer JSON is leeg.';
      return;
    }

    let body: any;
    try {
      body = JSON.parse(this.transferJsonText);
    } catch (e) {
      this.transferError = 'Transfer JSON is geen geldige JSON.';
      return;
    }

    this.transferLoading = true;
    this.transferError = null;
    this.transferResponse = null;

    this.catalogService.startTransfer(body).subscribe({
      next: (resp) => {
        this.transferResponse = resp;
        this.transferLoading = false;
      },
      error: (err) => {
        console.error('Transfer error', err);
        this.transferError = 'Transfer request faalde (zie console voor details).';
        this.transferLoading = false;
      }
    });
  }
}
