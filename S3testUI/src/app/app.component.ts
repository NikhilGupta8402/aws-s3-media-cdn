import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';

interface MediaFile {

  imageUrl: string;

  thumbnailUrl?: string;

  type?: string;

  size?: string;

  fileName?: string;
} 

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
})
export class AppComponent implements OnInit {
  mediaFiles: MediaFile[] = [];

  selectedImage: string = '';

  loading: boolean = true;

  uploading: boolean = false;

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.loadImages();
  }

  /*
   * Load Gallery
   */
  loadImages(): void {
    this.http
      .get<MediaFile[]>('http://localhost:8080/api/files/all')
      .subscribe({
        next: (response) => {
          this.mediaFiles = response;

          this.loading = false;
        },

        error: (error) => {
          console.error(error);

          this.loading = false;
        },
      });
  }

  /*
   * Single Upload
   */
  uploadSingleFile(event: any): void {
    const file = event.target.files[0];

    if (!file) return;

    const formData = new FormData();

    formData.append('file', file);

    this.uploading = true;

    this.http
      .post('http://localhost:8080/api/files/upload', formData)
      .subscribe({
        next: () => {
          this.loadImages();

          this.uploading = false;
        },

        error: (error) => {
          console.error(error);

          this.uploading = false;
        },
      });
  }

  /*
   * Multiple Upload
   */
  uploadMultipleFiles(event: any): void {
    const files = event.target.files;

    if (!files.length) return;

    const formData = new FormData();

    for (let i = 0; i < files.length; i++) {
      formData.append('files', files[i]);
    }

    this.uploading = true;

    this.http
      .post('http://localhost:8080/api/files/upload/multiple', formData)
      .subscribe({
        next: () => {
          this.loadImages();

          this.uploading = false;
        },

        error: (error) => {
          console.error(error);

          this.uploading = false;
        },
      });
  }

  /*
   * Delete File
   */
  deleteFile(fileName: string): void {
    const confirmDelete = confirm('Are you sure you want to delete this file?');

    if (!confirmDelete) return;

    this.http
      .delete(`http://localhost:8080/api/files/delete?fileName=${fileName}`)
      .subscribe({
        next: () => {
          this.mediaFiles = this.mediaFiles.filter(
            (file) => file.fileName !== fileName,
          );
        },

        error: (error) => {
          console.error(error);
        },
      });
  }

  /*
   * Preview
   */
  openPreview(image: string): void {
    this.selectedImage = image;
  }

  closePreview(): void {
    this.selectedImage = '';
  }

  /*
   * Detect Video
   */
  isVideo(url: string): boolean {
    return (
      url.endsWith('.mp4') || url.endsWith('.webm') || url.endsWith('.mkv')
    );
  }
}
