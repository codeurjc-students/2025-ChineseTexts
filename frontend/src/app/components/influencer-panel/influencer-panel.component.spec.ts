import { TestBed, ComponentFixture } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';

import { InfluencerPanelComponent } from './influencer-panel.component';
import { InfluencersService, InfluencerCode } from '../../services/influencers.service';
import { translocoTesting } from '../../i18n/transloco-testing';

describe('InfluencerPanelComponent', () => {
  let component: InfluencerPanelComponent;
  let fixture: ComponentFixture<InfluencerPanelComponent>;
  let serviceSpy: jasmine.SpyObj<InfluencersService>;

  const stripeRow: InfluencerCode = {
    id: 'promo_1', code: 'MARIA20', active: true, percentOff: 20,
    duration: 'once', durationInMonths: null, firstTimeOnly: true, timesRedeemed: 3,
    signups: 5, conversions: 2, activePremium: 1
  };

  const refOnlyRow: InfluencerCode = {
    id: null, code: 'BLOG', active: null, percentOff: null,
    duration: null, durationInMonths: null, firstTimeOnly: null, timesRedeemed: null,
    signups: 4, conversions: 0, activePremium: 0
  };

  beforeEach(async () => {
    serviceSpy = jasmine.createSpyObj('InfluencersService',
      ['getStats', 'createCode', 'deactivateCode']);
    serviceSpy.getStats.and.returnValue(of([stripeRow, refOnlyRow]));

    await TestBed.configureTestingModule({
      imports: [translocoTesting(), InfluencerPanelComponent],
      providers: [{ provide: InfluencersService, useValue: serviceSpy }],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(InfluencerPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads the tracking table on init, including ref-only rows', () => {
    expect(serviceSpy.getStats).toHaveBeenCalled();
    expect(component.codes.length).toBe(2);
    expect(component.codes[1].id).toBeNull(); // ref-only campaign still listed
  });

  it('creates a code with the form values and reloads the table', () => {
    serviceSpy.createCode.and.returnValue(of({ ...stripeRow, code: 'PEDRO10' }));
    component.newCode = 'PEDRO10';
    component.newPercent = 10;
    component.newDuration = 'once';

    component.create();

    // The anti-farming guard travels ON by default (checkbox pre-ticked).
    expect(serviceSpy.createCode).toHaveBeenCalledWith({
      code: 'PEDRO10', percentOff: 10, duration: 'once', durationInMonths: null,
      firstTimeOnly: true
    });
    expect(serviceSpy.getStats).toHaveBeenCalledTimes(2); // init + reload
    expect(component.messageType).toBe('success');
    expect(component.newCode).toBe(''); // form reset
    expect(component.newFirstTimeOnly).toBeTrue(); // guard resets to the safe default
  });

  it('sends firstTimeOnly=false only when the admin unticks the guard (win-back)', () => {
    serviceSpy.createCode.and.returnValue(of({ ...stripeRow, code: 'VUELVE10', firstTimeOnly: false }));
    component.newCode = 'VUELVE10';
    component.newPercent = 10;
    component.newFirstTimeOnly = false;

    component.create();

    expect(serviceSpy.createCode).toHaveBeenCalledWith(
      jasmine.objectContaining({ firstTimeOnly: false }));
  });

  it('does not call the API while the form is incomplete', () => {
    component.newCode = 'PEDRO10';
    component.newPercent = null;
    component.create();

    // repeating without months is also incomplete
    component.newPercent = 10;
    component.newDuration = 'repeating';
    component.newMonths = null;
    component.create();

    expect(serviceSpy.createCode).not.toHaveBeenCalled();
  });

  it('maps a duplicate-code rejection (400 CODE_EXISTS) to its specific message', () => {
    serviceSpy.createCode.and.returnValue(throwError(() => ({
      status: 400, error: { code: 'CODE_EXISTS' }
    })));
    component.newCode = 'MARIA20';
    component.newPercent = 20;

    component.create();

    expect(component.messageType).toBe('error');
    // translocoTesting resolves real dictionaries, so we assert on the English text.
    expect(component.message).toContain('already exists');
  });

  it('deactivates only after inline confirmation and reloads', () => {
    serviceSpy.deactivateCode.and.returnValue(of(void 0));

    component.askDeactivate('promo_1');
    expect(serviceSpy.deactivateCode).not.toHaveBeenCalled();

    component.confirmDeactivate('promo_1');
    expect(serviceSpy.deactivateCode).toHaveBeenCalledWith('promo_1');
    expect(serviceSpy.getStats).toHaveBeenCalledTimes(2);
  });

  it('shows the billing-unavailable message when the backend answers 503', () => {
    serviceSpy.getStats.and.returnValue(throwError(() => ({ status: 503 })));
    component.load();
    expect(component.messageType).toBe('error');
    expect(component.message).toContain('Billing is not configured');
  });
});
