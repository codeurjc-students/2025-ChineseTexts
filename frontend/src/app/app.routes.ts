import { Routes } from '@angular/router';
import { HomeComponent } from './components/home/home.component';
import { TextsComponent } from './components/texts/texts.component';

export const routes: Routes = [
    { path: '', component: HomeComponent },
    { path: 'texts', component: TextsComponent },
];
