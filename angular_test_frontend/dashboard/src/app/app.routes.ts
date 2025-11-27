import { Routes } from '@angular/router';
import {Catalog} from './catalog/catalog';
import {Admin} from './admin/admin';

export const routes: Routes = [
  { path: '', redirectTo: 'catalog', pathMatch: 'full' },
  { path: 'catalog', component: Catalog },
  { path: 'admin', component: Admin },
  { path: '**', redirectTo: 'catalog' }
];
