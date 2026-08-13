import { VisionService } from '../src/multimodal/vision_service.js';
import assert from 'assert';

describe('VisionService Tests', () => {
  it('should initialize successfully with valid API key', () => {
    const service = new VisionService('AIzaSyDummyKeyForTestingPurposesOnly');
    assert.strictEqual(typeof service.analyzeFrame, 'function');
  });
});
