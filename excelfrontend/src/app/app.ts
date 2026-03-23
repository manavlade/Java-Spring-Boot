import { Component, signal } from '@angular/core';
import { RouterLinkActive, RouterOutlet, RouterLink } from '@angular/router';
import { Upload } from './components/upload/upload';
import { HlmButtonImports } from '@spartan-ng/helm/button';

@Component({
  selector: 'app-root',
  imports: [
    Upload,
    RouterOutlet,
    HlmButtonImports,
    RouterLink,
    RouterLinkActive
  ],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('excelfrontend');
}
