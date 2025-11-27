import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CatalogService } from '../services/catalog.service';

@Component({
  selector: 'app-catalog-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin.html',
  styleUrls: ['./admin.css']
})
export class Admin {

  assetJson = `{
  "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
  "@id": "10",
  "properties": {
    "name": "Test document",
    "description": "Azure blob test-document.txt in src-container"
  },
  "dataAddress": {
    "type": "AzureStorage",
    "account": "provider",
    "container": "src-container",
    "blobName": "test-document.txt",
    "keyName": "provider-key"
  }
}`;

  policyJson = `{
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
    "odrl": "http://www.w3.org/ns/odrl/2/"
  },
  "@id": "10",
  "policy": {
    "@context": "http://www.w3.org/ns/odrl.jsonld",
    "@type": "Set",
    "permission": [],
    "prohibition": [],
    "obligation": []
  }
}`;


  contractJson = `{
  "@context": { "@vocab": "https://w3id.org/edc/v0.0.1/ns/" },
  "@id": "10",
  "accessPolicyId": "10",
  "contractPolicyId": "10",
  "assetsSelector": [
    {
      "operandLeft": "https://w3id.org/edc/v0.0.1/ns/id",
      "operator": "=",
      "operandRight": "10"
    }
  ]
}`;

  loading = false;
  lastResponse: any = null;
  lastError: string | null = null;

  constructor(private catalog: CatalogService) {}

  private parseJSON(input: string): any | null {
    try {
      return JSON.parse(input);
    } catch (err: any) {
      this.lastError = 'Invalid JSON: ' + err.message;
      this.lastResponse = null;
      this.loading = false;
      return null;
    }
  }

  createAsset() {
    this.loading = true;
    this.lastError = null;
    const body = this.parseJSON(this.assetJson);
    if (!body) return;

    this.catalog.createAsset(body).subscribe({
      next: res => { this.lastResponse = res; this.loading = false; },
      error: err => { this.lastError = 'Error creating asset: ' + (err.message || err.statusText); this.loading = false; }
    });
  }

  createPolicy() {
    this.loading = true;
    this.lastError = null;
    const body = this.parseJSON(this.policyJson);
    if (!body) return;

    this.catalog.createPolicy(body).subscribe({
      next: res => { this.lastResponse = res; this.loading = false; },
      error: err => { this.lastError = 'Error creating policy: ' + (err.message || err.statusText); this.loading = false; }
    });
  }

  createContract() {
    this.loading = true;
    this.lastError = null;
    const body = this.parseJSON(this.contractJson);
    if (!body) return;

    this.catalog.createContractDefinition(body).subscribe({
      next: res => { this.lastResponse = res; this.loading = false; },
      error: err => { this.lastError = 'Error creating contract definition: ' + (err.message || err.statusText); this.loading = false; }
    });
  }

  // optional helper if you want to run all three with one click
  runAll() {
    this.lastError = null;
    this.lastResponse = null;
    this.loading = true;

    const assetBody = this.parseJSON(this.assetJson);
    const policyBody = this.parseJSON(this.policyJson);
    const contractBody = this.parseJSON(this.contractJson);
    if (!assetBody || !policyBody || !contractBody) return;

    this.catalog.createAsset(assetBody).subscribe({
      next: assetRes => {
        this.catalog.createPolicy(policyBody).subscribe({
          next: policyRes => {
            this.catalog.createContractDefinition(contractBody).subscribe({
              next: contractRes => {
                this.lastResponse = {
                  assetResponse: assetRes,
                  policyResponse: policyRes,
                  contractResponse: contractRes
                };
                this.loading = false;
              },
              error: err => {
                this.lastError = 'Error creating contract definition: ' + (err.message || err.statusText);
                this.loading = false;
              }
            });
          },
          error: err => {
            this.lastError = 'Error creating policy: ' + (err.message || err.statusText);
            this.loading = false;
          }
        });
      },
      error: err => {
        this.lastError = 'Error creating asset: ' + (err.message || err.statusText);
        this.loading = false;
      }
    });
  }
}
