import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Testingnew } from './testingnew';

describe('Testingnew', () => {
  let component: Testingnew;
  let fixture: ComponentFixture<Testingnew>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Testingnew],
    }).compileComponents();

    fixture = TestBed.createComponent(Testingnew);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
