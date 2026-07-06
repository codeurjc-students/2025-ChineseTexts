import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';

import { CollectionsComponent } from './collections.component';
import { CollectionsService, CollectionDTO, FlashcardDTO } from '../../services/collections.service';
import { LoginService } from '../../services/login.service';

import { translocoTesting } from "../../i18n/transloco-testing";

describe('CollectionsComponent', () => {
  let component: CollectionsComponent;
  let fixture: ComponentFixture<CollectionsComponent>;
  let collectionsServiceSpy: jasmine.SpyObj<CollectionsService>;
  let loginServiceSpy: jasmine.SpyObj<LoginService>;

  const mockCollections: CollectionDTO[] = [
    { id: 1, title: 'HSK1 Words', date: '2025-01-01' },
    { id: 2, title: 'HSK2 Words', date: '2025-02-01' }
  ];

  const mockFlashcards: FlashcardDTO[] = [
    {
      id: 1,
      word: { id: 1, chinese: '你好', pinyin: 'nǐ hǎo', english: 'Hello', spanish: 'Hola' },
      example: { id: 1, titleEnglish: 'My Friend' },
      collection: mockCollections[0]
    }
  ];

  beforeEach(async () => {
    collectionsServiceSpy = jasmine.createSpyObj('CollectionsService',
      ['getUserCollections', 'getCollectionFlashcards', 'createCollection',
       'deleteCollection', 'deleteFlashcard']);
    loginServiceSpy = jasmine.createSpyObj('LoginService',
      ['isLogged', 'reqIsLogged'], { loggedIn$: of(true) });

    loginServiceSpy.isLogged.and.returnValue(true);
    loginServiceSpy.reqIsLogged.and.returnValue(of({ email: 'user@test.com', roles: ['USER'] } as any));
    collectionsServiceSpy.getUserCollections.and.returnValue(of(mockCollections));

    await TestBed.configureTestingModule({
      imports: [translocoTesting(), CollectionsComponent, RouterTestingModule],
      providers: [
        { provide: CollectionsService, useValue: collectionsServiceSpy },
        { provide: LoginService, useValue: loginServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CollectionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // Test unitario 1: El componente se crea y carga las colecciones
  it('should create and load collections on init', () => {
    expect(component).toBeTruthy();
    expect(collectionsServiceSpy.getUserCollections).toHaveBeenCalled();
    expect(component.collections.length).toBe(2);
  });

  // Test unitario 2: toggleCollectionSelection selecciona y deselecciona colecciones
  it('should toggle collection selection', () => {
    component.toggleCollectionSelection(1);
    expect(component.isSelected(1)).toBeTrue();

    component.toggleCollectionSelection(1);
    expect(component.isSelected(1)).toBeFalse();
  });

  // Test unitario 3: canStartStudyOrExam es false si no hay colecciones seleccionadas
  it('should not allow study or exam when no collection is selected', () => {
    expect(component.canStartStudyOrExam).toBeFalse();
  });

  // Test unitario 4: canStartStudyOrExam es true si hay al menos una colección seleccionada
  it('should allow study or exam when at least one collection is selected', () => {
    component.toggleCollectionSelection(1);
    expect(component.canStartStudyOrExam).toBeTrue();
  });

  // Test unitario 5: selectCollection carga las flashcards y cambia el modo a detail
  it('should load flashcards and switch to detail mode when a collection is selected', () => {
    collectionsServiceSpy.getCollectionFlashcards.and.returnValue(of(mockFlashcards));

    component.selectCollection(mockCollections[0]);

    expect(collectionsServiceSpy.getCollectionFlashcards).toHaveBeenCalledWith(1);
    expect(component.mode).toBe('detail');
    expect(component.flashcards.length).toBe(1);
  });

  // Test unitario 6: backToList resetea el estado
  it('should reset state when going back to list', () => {
    component.selectedCollection = mockCollections[0];
    component.mode = 'detail';
    component.toggleCollectionSelection(1);

    component.backToList();

    expect(component.mode).toBe('list');
    expect(component.selectedCollection).toBeNull();
    expect(component.selectedCollectionIds.size).toBe(0);
  });

  // Test unitario 7: openAddModal abre el modal de nueva colección
  it('should open the add collection modal', () => {
    component.openAddModal();
    expect(component.showAddModal).toBeTrue();
    expect(component.newCollectionTitle).toBe('');
  });
});