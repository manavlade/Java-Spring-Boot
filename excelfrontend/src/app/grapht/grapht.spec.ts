import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Grapht } from './grapht';

describe('Grapht', () => {
  let component: Grapht;
  let fixture: ComponentFixture<Grapht>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Grapht],
    }).compileComponents();

    fixture = TestBed.createComponent(Grapht);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
