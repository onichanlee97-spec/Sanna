import { GoogleGenerativeAI } from '@google/generative-ai';

export class VisionService {
  constructor(apiKey) {
    const genAI = new GoogleGenerativeAI(apiKey);
    this.model = genAI.getGenerativeModel({ model: 'gemini-1.5-flash' });
  }

  async analyzeFrame(base64Image, promptText = 'Describe this image in detail.') {
    try {
      const imagePart = {
        inlineData: {
          data: base64Image,
          mimeType: 'image/jpeg',
        },
      };

      const result = await this.model.generateContent([promptText, imagePart]);
      const response = await result.response;
      return response.text();
    } catch (error) {
      console.error('Vision analysis failed:', error);
      throw error;
    }
  }
}
