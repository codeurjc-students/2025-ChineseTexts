import { Routes } from '@angular/router';
import { HomeComponent } from './components/home/home.component';
import { TextsComponent } from './components/texts/texts.component';
import { SignupComponent } from './components/signup/signup.component';
import { SuccessComponent } from './components/success/success.component';
import { ErrorComponent } from './components/error/error.component';
import { TextComponent } from './components/text/text.component';
import { CollectionsComponent } from './components/collections/collections.component';
import { UploadTextComponent } from './components/upload-text/upload-text.component';
import { ProfileComponent } from './components/profile/profile.component';
import { AiToolsComponent } from './components/ai-tools/ai-tools.component';
import { AdminToolsComponent } from './components/admin-tools/admin-tools.component';
import { PrivacyPolicyComponent } from './components/privacy-policy/privacy-policy.component';
import { TermsOfUseComponent } from './components/terms-of-use/terms-of-use.component';
import { FounderComponent } from './components/founder/founder.component';
import { MyToolsComponent } from './components/my-tools/my-tools.component';
import { MyTextReaderComponent } from './components/my-text-reader/my-text-reader.component';
import { PremiumComponent } from './components/premium/premium.component';
import { PremiumSuccessComponent } from './components/premium-success/premium-success.component';
import { LevelTestComponent } from './components/level-test/level-test.component';
import { NotFoundComponent } from './components/not-found/not-found.component';

/**
 * The application's routes, defined once. English is served from the root and
 * Spanish is served from the same components under an `/es` prefix (see below),
 * so there is a single source of truth and no component duplication.
 */
const appRoutes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'texts', component: TextsComponent },
  { path: 'texts/:level', component: TextsComponent },
  { path: 'signup', component: SignupComponent },
  { path: 'success', component: SuccessComponent },
  { path: 'error', component: ErrorComponent },
  { path: 'text/:id', component: TextComponent },
  { path: 'collections', component: CollectionsComponent },
  { path: 'upload-text', component: UploadTextComponent },
  { path: 'profile', component: ProfileComponent },
  { path: 'ai-tools', component: AiToolsComponent},
  { path: 'admin-tools', component: AdminToolsComponent },
  { path: 'my-tools', component: MyToolsComponent },
  { path: 'my-text/:id', component: MyTextReaderComponent },
  { path: 'privacy-policy', component: PrivacyPolicyComponent },
  { path: 'terms-of-use', component: TermsOfUseComponent },
  { path: 'founder', component: FounderComponent },
  { path: 'premium', component: PremiumComponent },
  { path: 'premium/success', component: PremiumSuccessComponent },
  { path: 'level-test', component: LevelTestComponent },
];

export const routes: Routes = [
  ...appRoutes,
  // Spanish locale: same components, prefixed URLs (`/es`, `/es/texts`, …).
  // The active language is resolved from this prefix in AppComponent.
  // Each level gets its own `**` wildcard (a not-found page) so unknown URLs
  // render a proper 404 page instead of failing to navigate. The Spanish one
  // must live INSIDE the `es` children: a top-level wildcard placed before
  // `es` would swallow every /es URL.
  { path: 'es', children: [...appRoutes, { path: '**', component: NotFoundComponent }] },
  { path: '**', component: NotFoundComponent },
];