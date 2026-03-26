import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Graphsnew } from './graphsnew';

describe('Graphsnew', () => {
  let component: Graphsnew;
  let fixture: ComponentFixture<Graphsnew>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Graphsnew],
    }).compileComponents();

    fixture = TestBed.createComponent(Graphsnew);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
