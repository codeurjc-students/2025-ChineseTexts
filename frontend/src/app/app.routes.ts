import { Routes } from '@angular/router';
import { HomeComponent } from './components/home/home.component';
import { TextsComponent } from './components/texts/texts.component';
import { SignupComponent } from './components/signup/signup.component';
import { SuccessComponent } from './components/success/success.component';
import { ErrorComponent } from './components/error/error.component';
import { TextComponent } from './components/text/text.component';
import { CollectionsComponent } from './components/collections/collections.component';
import { UploadTextComponent } from './components/upload-text/upload-text.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'texts', component: TextsComponent },
  { path: 'texts/:level', component: TextsComponent },
  { path: 'signup', component: SignupComponent },
  { path: 'success', component: SuccessComponent },
  { path: 'error', component: ErrorComponent },
  { path: 'text/:id', component: TextComponent },
  { path: 'collections', component: CollectionsComponent },
  { path: 'upload-text', component: UploadTextComponent },
];