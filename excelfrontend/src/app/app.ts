import { Component } from '@angular/core';
import { RouterLinkActive, RouterOutlet, RouterLink } from '@angular/router';
import html2canvas from 'html2canvas-pro';
import { jsPDF } from 'jspdf';

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


  async handleDownload() {
    const element = document.getElementById('home');
    if (!element) return;

    const pdf = new jsPDF({
      orientation: 'portrait',
      unit: 'mm',
      format: 'a4',
    });

    const MARGIN = 10;g
    const HEADER_MM = 12;
    const FOOTER_MM = 10;
    const pageWidth = pdf.internal.pageSize.getWidth();
    const pageHeight = pdf.internal.pageSize.getHeight();
    const contentWidth = pageWidth - MARGIN * 2;

    const headerEl = element.querySelector<HTMLElement>('.pdf-header');
    const footerEl = element.querySelector<HTMLElement>('.pdf-footer');

    if (headerEl) headerEl.style.display = 'flex';
    if (footerEl) footerEl.style.display = 'flex';

    const headerCanvas = headerEl
      ? await html2canvas(headerEl, { scale: 2, useCORS: true, logging: false })
      : null;

    const footerCanvas = footerEl
      ? await html2canvas(footerEl, { scale: 2, useCORS: true, logging: false })
      : null;

    if (headerEl) headerEl.style.display = 'none';
    if (footerEl) footerEl.style.display = 'none';

    const headerImgData = headerCanvas?.toDataURL('image/png') ?? null;
    const footerImgData = footerCanvas?.toDataURL('image/png') ?? null;

    const drawHeaderFooter = () => {
      if (headerImgData) {
        pdf.addImage(headerImgData, 'PNG', MARGIN, MARGIN, contentWidth, HEADER_MM);
      }
      if (footerImgData) {
        pdf.addImage(footerImgData, 'PNG', MARGIN, pageHeight - MARGIN - FOOTER_MM, contentWidth, FOOTER_MM);
      }
    };

    const contentStartY = MARGIN + HEADER_MM + 2;
    const contentEndY = pageHeight - MARGIN - FOOTER_MM - 2;
    const availableHeight = contentEndY - contentStartY;

    const sections = element.querySelectorAll<HTMLElement>(
      'section, article, .report-section'
    );

    let currentY = contentStartY;

    drawHeaderFooter();

    for (const section of Array.from(sections)) {

      section.querySelectorAll<HTMLElement>('*').forEach(el => {

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

      const canvas = await html2canvas(section, {
        scale: 2,
        useCORS: true,
        logging: false,
      });

      const imgData = canvas.toDataURL('image/png');
      const imgWidth = contentWidth;
      const imgHeight = (canvas.height * imgWidth) / canvas.width;

      const spaceLeft = contentEndY - currentY;

      if (currentY > contentStartY && imgHeight > spaceLeft) {
        pdf.addPage();
        drawHeaderFooter();
        currentY = contentStartY;
      }

      pdf.addImage(imgData, 'PNG', MARGIN, currentY, imgWidth, imgHeight);
      currentY += imgHeight + MARGIN;

      if (imgHeight > availableHeight) {
        let remainingHeight = imgHeight - availableHeight;
        let offsetY = availableHeight;

        while (remainingHeight > 0) {
          pdf.addPage();
          drawHeaderFooter();
          pdf.addImage(imgData, 'PNG', MARGIN, contentStartY - offsetY, imgWidth, imgHeight);
          offsetY += availableHeight;
          remainingHeight -= availableHeight;
        }

        currentY = contentStartY + (imgHeight % availableHeight);
      }
    }

    const textContent = element.innerText;
    pdf.setFontSize(0);
    pdf.setTextColor(0, 0, 0);
    pdf.text(textContent, 0, 0);

    pdf.save('document.pdf');
  }
}