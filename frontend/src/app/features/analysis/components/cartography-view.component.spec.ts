import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { describe, it, expect, beforeEach } from 'vitest';

import { CartographyViewComponent } from './cartography-view.component';
import { CartographyResponse } from '../../../core/models/analysis.model';

const CARTOGRAPHY_WITH_UNKNOWNS: CartographyResponse = {
  controllerRef: 'Ctrl',
  fxmlRef: 'src/main/resources/view/Main.fxml',
  hasUnknowns: true,
  components: [
    { fxId: 'rootPane', componentType: 'AnchorPane', eventHandler: '' },
    { fxId: 'saveButton', componentType: 'Button', eventHandler: 'onSave' },
  ],
  handlers: [
    { methodName: 'onSave', fxmlRef: '#saveButton', injectedType: 'ActionEvent' },
  ],
};

const EMPTY_CARTOGRAPHY: CartographyResponse = {
  controllerRef: 'Ctrl',
  fxmlRef: 'src/main/resources/view/Empty.fxml',
  hasUnknowns: false,
  components: [],
  handlers: [],
};

describe('CartographyViewComponent', () => {
  beforeEach(() => {
    TestBed.resetTestingModule();
  });

  it('should render counts, table rows and unknown banner when data is present', async () => {
    await TestBed.configureTestingModule({
      imports: [CartographyViewComponent],
    }).compileComponents();

    const fixture = TestBed.createComponent(CartographyViewComponent);
    fixture.componentRef.setInput('data', CARTOGRAPHY_WITH_UNKNOWNS);
    fixture.detectChanges();

    const badges = fixture.debugElement.queryAll(By.css('.count-badge'));
    expect(badges[0].nativeElement.textContent).toContain('2 composants');
    expect(badges[1].nativeElement.textContent).toContain('1 handlers');

    const warningBanner = fixture.debugElement.query(By.css('.warning-banner'));
    expect(warningBanner).toBeTruthy();
    expect(warningBanner.nativeElement.textContent).toContain('elements inconnus');

    const componentRows = fixture.debugElement.queryAll(By.css('tbody tr'));
    expect(componentRows.length).toBe(3);
    expect(componentRows[0].nativeElement.textContent).toContain('rootPane');
    expect(componentRows[0].nativeElement.textContent).toContain('-');
    expect(componentRows[1].nativeElement.textContent).toContain('saveButton');
    expect(componentRows[2].nativeElement.textContent).toContain('onSave');
    expect(componentRows[2].nativeElement.textContent).toContain('ActionEvent');
  });

  it('should show empty messages when no components or handlers are detected', async () => {
    await TestBed.configureTestingModule({
      imports: [CartographyViewComponent],
    }).compileComponents();

    const fixture = TestBed.createComponent(CartographyViewComponent);
    fixture.componentRef.setInput('data', EMPTY_CARTOGRAPHY);
    fixture.detectChanges();

    const warningBanner = fixture.debugElement.query(By.css('.warning-banner'));
    expect(warningBanner).toBeNull();

    const emptyMessages = fixture.debugElement.queryAll(By.css('.empty-msg'));
    expect(emptyMessages.length).toBe(2);
    expect(emptyMessages[0].nativeElement.textContent).toContain('Aucun composant detecte');
    expect(emptyMessages[1].nativeElement.textContent).toContain('Aucun handler detecte');
    expect(fixture.debugElement.query(By.css('th'))).toBeNull();
  });
});
