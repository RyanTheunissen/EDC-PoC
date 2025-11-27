import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CatalogRequest {
  '@context': {
    '@vocab': string;
  };
  counterPartyAddress: string;
  protocol: string;
}

@Injectable({
  providedIn: 'root'
})
export class CatalogService {
  private consumerBaseUrl = '/management/v3';
  private providerBaseUrl = '/provider/management/v3';
  private apiKey = 'password';

  constructor(private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    return new HttpHeaders({
      'X-Api-Key': this.apiKey,
      'Content-Type': 'application/json'
    });
  }

  fetchCatalog(request: CatalogRequest): Observable<any> {
    const url = `${this.consumerBaseUrl}/catalog/request`;
    return this.http.post<any>(url, request, { headers: this.getHeaders() });
  }

  negotiateContract(body: any): Observable<any> {
    const url = `${this.consumerBaseUrl}/contractnegotiations`;
    return this.http.post<any>(url, body, { headers: this.getHeaders() });
  }

  getNegotiation(id: string): Observable<any> {
    const url = `${this.consumerBaseUrl}/contractnegotiations/${encodeURIComponent(id)}`;
    return this.http.get<any>(url, { headers: this.getHeaders() });
  }

  startTransfer(body: any): Observable<any> {
    const url = `${this.consumerBaseUrl}/transferprocesses`;
    return this.http.post<any>(url, body, { headers: this.getHeaders() });
  }

  // ------------------------------
  // Provider-side Catalog Creation
  // ------------------------------

  createAsset(body: any): Observable<any> {
    const url = `${this.providerBaseUrl}/assets`;
    return this.http.post<any>(url, body, { headers: this.getHeaders() });
  }

  createPolicy(body: any): Observable<any> {
    const url = `${this.providerBaseUrl}/policydefinitions`;
    return this.http.post<any>(url, body, { headers: this.getHeaders() });
  }

  createContractDefinition(body: any): Observable<any> {
    const url = `${this.providerBaseUrl}/contractdefinitions`;
    return this.http.post<any>(url, body, { headers: this.getHeaders() });
  }

}
