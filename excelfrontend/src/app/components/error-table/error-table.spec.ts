import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ErrorTable } from './error-table';

describe('ErrorTable', () => {
  let component: ErrorTable;
  let fixture: ComponentFixture<ErrorTable>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ErrorTable],
    }).compileComponents();

    fixture = TestBed.createComponent(ErrorTable);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
