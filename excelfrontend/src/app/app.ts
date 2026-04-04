import { Component, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterLinkActive, RouterOutlet, RouterLink } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive
  ],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {

  private readonly platformId = inject(PLATFORM_ID);

  async downloadPDF() {
    if (!isPlatformBrowser(this.platformId)) return;

    const element = document.getElementById('home');
    if (!element) return;

    const clone = element.cloneNode(true) as HTMLElement;
    Object.assign(clone.style, {
      position: 'fixed',
      top: '0',
      left: '-9999px',
      width: element.offsetWidth + 'px',
      zIndex: '-1',
      pointerEvents: 'none',
    });
    document.body.appendChild(clone);

    const allElements = Array.from(clone.querySelectorAll<HTMLElement>('*'));
    allElements.forEach((el) => {
      if (el.tagName === 'INPUT' && (el as HTMLInputElement).type === 'checkbox') {
        el.style.display = 'none';
        return;
      }
      const computed = globalThis.getComputedStyle(el);
      const isHidden =
        computed.display === 'none' ||
        computed.visibility === 'hidden' ||
        computed.opacity === '0' ||
        (Number.parseFloat(computed.maxHeight) === 0 && computed.overflow === 'hidden') ||
        (Number.parseFloat(computed.height) === 0 && computed.overflow === 'hidden');

      if (isHidden) {
        el.style.display = computed.display === 'none' ? 'block' : computed.display;
        el.style.visibility = 'visible';
        el.style.opacity = '1';
        el.style.maxHeight = 'none';
        el.style.height = 'auto';
        el.style.overflow = 'visible';
      }
    });

    const originalCanvases = Array.from(element.querySelectorAll<HTMLCanvasElement>('canvas'));
    const clonedCanvases = Array.from(clone.querySelectorAll<HTMLCanvasElement>('canvas'));

    originalCanvases.forEach((original, i) => {
      const cloned = clonedCanvases[i];
      if (!cloned) return;

      cloned.width = original.width;
      cloned.height = original.height;

      const ctx = cloned.getContext('2d');
      if (ctx) {
        ctx.drawImage(original, 0, 0);
      }
    });
    await new Promise((r) => requestAnimationFrame(r));
    await new Promise((r) => requestAnimationFrame(r));

    const [{ default: html2canvas }, { jsPDF }] = await Promise.all([
      import('html2canvas-pro'),
      import('jspdf'),
    ]);

    const { getInstanceByDom } = await import('echarts');
    const chartEl = document.getElementById('mainChart');
    const chartInstance = chartEl ? getInstanceByDom(chartEl) : null;

    if (chartInstance) {
      chartInstance.dispatchAction({ type: 'showTip', seriesIndex: 0, dataIndex: 2 });
      await new Promise((r) => setTimeout(r, 300));

      originalCanvases.forEach((original, i) => {
        const cloned = clonedCanvases[i];
        if (!cloned) return;
        cloned.width = original.width;
        cloned.height = original.height;
        const ctx = cloned.getContext('2d');
        if (ctx) ctx.drawImage(original, 0, 0);
      });
    }

    const canvas = await html2canvas(clone, {
      scale: 2,
      useCORS: true,
      logging: false,
      onclone: (clonedDoc) => {
        const tooltips = clonedDoc.querySelectorAll<HTMLElement>('.echarts-tooltip');
        tooltips.forEach((tooltip) => {
          tooltip.style.transition = 'none';
          tooltip.style.opacity = '1';
          tooltip.style.visibility = 'visible';
          tooltip.style.display = 'block';
        });
      }
    });

    if (chartInstance) {
      chartInstance.dispatchAction({ type: 'hideTip' });
    }

    const pdf = new jsPDF({ unit: 'mm', format: 'a4', orientation: 'portrait' });
    const headerHeight = 10;
    let pageNumber = 1;
    const pageWidthMm = pdf.internal.pageSize.getWidth();
    const pageHeightMm = pdf.internal.pageSize.getHeight();
    const margin = 10;
    const contentWidth = pageWidthMm - margin * 2;

    const contentHeightMm = pageHeightMm - margin * 2 - headerHeight;
    const domScale = canvas.width / clone.offsetWidth;
    const pageHeightPx = contentHeightMm * (canvas.width / contentWidth);
    const children = Array.from(

      clone.querySelectorAll<HTMLElement>('section, article, .report-section')
    );

    const safeBreakPoints = new Set<number>();
    children.forEach((el) => {
      const bottom = (el.offsetTop + el.offsetHeight) * domScale;
      if (el.offsetHeight > 0) {
        safeBreakPoints.add(Math.round(bottom));
      }
    });

    const sortedBreaks = Array.from(safeBreakPoints).sort((a, b) => a - b);

    document.body.removeChild(clone);

    let currentYpx = 0;

    while (currentYpx < canvas.height) {
      let sliceBottomPx = currentYpx + pageHeightPx;

      const safeBreak = sortedBreaks
        .filter((b) => b > currentYpx && b <= sliceBottomPx)
        .pop();

      if (safeBreak) sliceBottomPx = safeBreak;

      sliceBottomPx = Math.min(sliceBottomPx, canvas.height);

      const sliceHeightPx = sliceBottomPx - currentYpx;
      const sliceHeightMm = (sliceHeightPx * contentWidth) / canvas.width;

      const sliceCanvas = document.createElement('canvas');
      sliceCanvas.width = canvas.width;
      sliceCanvas.height = sliceHeightPx;

      const ctx = sliceCanvas.getContext('2d')!;
      ctx.drawImage(canvas, 0, currentYpx, canvas.width, sliceHeightPx, 0, 0, canvas.width, sliceHeightPx);

      const sliceData = sliceCanvas.toDataURL('image/jpeg', 0.98);
      pdf.setFontSize(5);
      pdf.setTextColor(0);
      pdf.text('ExcelFlow Report', margin, margin);
      pdf.text('Generated Report', pageWidthMm - margin, margin, { align: 'right' });

      pdf.addImage(sliceData, 'JPEG', margin, margin + headerHeight, contentWidth, sliceHeightMm);
      const footerY = pageHeightMm - 5;
      pdf.setFontSize(5);
      pdf.setTextColor('#999');
      pdf.text('© 2026 ExcelFlow. All rights   reserved.', margin, footerY);
      pdf.text('Privacy Terms', pageWidthMm - margin, footerY, {
        align: 'right'
      });
      pdf.text(`Page ${pageNumber}`, pageWidthMm / 2, footerY, { align: 'center' });

      currentYpx = sliceBottomPx;
      if (currentYpx < canvas.height) {
        pdf.addPage();
        pageNumber++;
      }
    }
    pdf.save('home.pdf');
  }
}