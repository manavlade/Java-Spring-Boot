import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EchartsGraph } from './echarts-graph';

describe('EchartsGraph', () => {
  let component: EchartsGraph;
  let fixture: ComponentFixture<EchartsGraph>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EchartsGraph],
    }).compileComponents();

    fixture = TestBed.createComponent(EchartsGraph);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
