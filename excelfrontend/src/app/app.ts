import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Upload } from './components/upload/upload';

@Component({
  selector: 'app-root',
  imports: [Upload],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('excelfrontend');
}
