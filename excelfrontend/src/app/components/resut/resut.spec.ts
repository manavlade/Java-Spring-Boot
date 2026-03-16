import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Resut } from './resut';

describe('Resut', () => {
  let component: Resut;
  let fixture: ComponentFixture<Resut>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Resut],
    }).compileComponents();

    fixture = TestBed.createComponent(Resut);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
