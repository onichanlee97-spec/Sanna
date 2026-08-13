import { VisionService } from './vision_service.js';

export class CameraStreamController {
  constructor(apiKey) {
    this.visionService = new VisionService(apiKey);
    this.isStreaming = false;
  }

  async startStream(intervalMs = 5000, onAnalysisCallback) {
    this.isStreaming = true;
    this.captureLoop(intervalMs, onAnalysisCallback);
  }

  async captureLoop(intervalMs, callback) {
    while (this.isStreaming) {
      const frameBase64 = await this.grabCameraBuffer();
      if (frameBase64) {
        const analysis = await this.visionService.analyzeFrame(frameBase64, 'Analyze current scene for objects and text.');
        callback(analysis);
      }
      await new Promise(resolve => setTimeout(resolve, intervalMs));
    }
  }

  async grabCameraBuffer() {
    return null;
  }

  stopStream() {
    this.isStreaming = false;
  }
}
