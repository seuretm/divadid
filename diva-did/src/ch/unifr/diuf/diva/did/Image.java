/*****************************************************
  DIVADid
  
  A document image degradation method.
  ------------------------------
  Author:
  2015 by Mathias Seuret <mathias.seuret@unifr.ch>
  -------------------

  This software is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation version 3.

  This software is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with this software; if not, write to the Free Software
  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 ******************************************************************************/
package ch.unifr.diuf.diva.did;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * This class stores an image as a multilayer array.
 *
 * @author Mathias Seuret
 */
public class Image {

    
    /**
     * Different image types.
     */
    public enum Type {
        RGB,
        YUV,
        HSV
    }
    /**
     * Stores the type of the image.
     */
    protected Type[] type;

    protected int width;

    public int getWidth() {
        return width;
    }
    protected int height;

    public int getHeight() {
        return height;
    }
    /**
     * Stores the values of the pixels, encoded in the format specified by the
     * variable type.
     */
    protected float[][][] pixel = null;
    
    protected float[][] weight;

    /**
     * Loads an image.
     * @param fname file name of the image
     * @throws IOException if the file could not be loaded
     */
    public Image(String fname) throws IOException {
        // opening the image
        BufferedImage bi;
        
        try {
            bi = ImageIO.read(new File(fname));
        } catch (IOException e) {
            System.err.println("Error while loading "+fname);
            throw e;
        }
        

        // creating the array
        width = bi.getWidth();
        height = bi.getHeight();
        pixel = new float[width][height][3];
        weight = new float[width][height];

        // loading data
        if (bi.getType()==BufferedImage.TYPE_BYTE_GRAY) {
            System.out.println("Loading graylevel image");
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int rgb = bi.getRGB(x, y);

                    pixel[x][y][0] = (rgb & 0x000000FF) / 255.0f;
                    pixel[x][y][1] = (rgb & 0x000000FF) / 255.0f;
                    pixel[x][y][2] = (rgb & 0x000000FF) / 255.0f;
                    
                    pixel[x][y][0] = (rgb & 0x0000FF) / 255.0f;
                    pixel[x][y][1] = (rgb & 0x0000FF) / 255.0f;
                    pixel[x][y][2] = (rgb & 0x0000FF) / 255.0f;
                }
            }
        } else {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int rgb = bi.getRGB(x, y);

                    pixel[x][y][0] = ((rgb & 0x00FF0000) >> 16) / 255.0f;
                    pixel[x][y][1] = ((rgb & 0x0000FF00) >> 8)  / 255.0f;
                    pixel[x][y][2] =  (rgb & 0x000000FF)        / 255.0f;
                }
            }
        }

        // we have an RGB image now
        type = new Type[height];
        for (int y=0; y<height; y++) {
            type[y] = Type.RGB;
        }
    }
    
    /**
     * Constructs an empty image.
     * @param w width
     * @param h  height
     */
    public Image(int w, int h) {
        
        width = w;
        height = h;
        pixel = new float[width][height][3];
        weight = new float[width][height];
        type = new Type[height];
        for (int y=0; y<height; y++) {
            type[y] = Type.RGB;
        }
    }
    
    public Image(Image src) {
        this(src.width, src.height);
        for (int x=0; x<src.width; x++) {
            for (int y=0; y<src.height; y++) {
                for (int l=0; l<3; l++) {
                    pixel[x][y][l] = src.pixel[x][y][l];
                }
            }
        }
    }
    
    public void write(String result) throws IOException {
        if (result.endsWith("jpg")) {
            write(result, "jpg");
        } else if (result.endsWith("png")) {
            write(result, "png");
        } else {
            throw new IllegalArgumentException(
                    "Unknown extension for "+result+"; use either .jpg or .png"
            );
        }
    }
    
    /**
     * Save the image to a file.
     * @param result file name
     * @throws IOException  if the file could not be written
     */
    public void write(String result, String format) throws IOException {
        // going to RGB format
        //toRGB();
        
        // building a buffered image
        BufferedImage bi = new BufferedImage(width,
                                             height,
                                             BufferedImage.TYPE_INT_RGB);
        for (int x=0; x<width; x++) {
            for (int y=0; y<height; y++) {
                int color = 0x00;
                for (int layer=0; layer<3; layer++) {
                    int component = (int)(256*(pixel[x][y][layer]));
                    if (component<0) {
                        component=0;
                    } else  if (component>255) {
                        component = 255;
                    }
                    color = (color<<8) | component;
                }
                bi.setRGB(x, y, color);
            }
        }
        
        // saving it
        ImageIO.write(bi, format, new File(result));
    }
    
    public void turnLeft() {
        float[][][] px = new float[height][width][getDepth()];
        int w          = width;
        int h          = height;
        
        for (int x=0; x<width; x++) {
            for (int y=0; y<height; y++) {
                int tx = y;
                int ty = width-1-x;
                for (int z=0; z<getDepth(); z++) {
                    px[tx][ty][z] = pixel[x][y][z];
                }
            }
        }
        
        pixel  = px;
        width  = h;
        height = w;
    }
    
    /**
     * Sets all pixels to (0,0,0)
     */
    public void toBlack() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                pixel[x][y][0] = 0.0f;
                pixel[x][y][1] = 0.0f;
                pixel[x][y][2] = 0.0f;
            }
        }
    }
    
    /**
     * Sets the given layer to black.
     * @param layer number
     */
    public void layerToBlack(int layer) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                pixel[x][y][layer] = 0.0f;
            }
        }
    }
    
    /**
     * Computes the mean value of red, green and blue, and assigns it to
     * all layers.
     */
    public void toGrayLevel() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                float r    = pixel[x][y][0];
                float g    = pixel[x][y][0];
                float b    = pixel[x][y][0];
                float gray = (r+g+b) / 3.0f;
                pixel[x][y][0] = gray;
                pixel[x][y][0] = gray;
                pixel[x][y][0] = gray;
            }
        }
    }
    
    /**
     * Returns the value of the corresponding layer and pixel. If it
     * is outside of the image, the closest value is returned - this avoids
     * some conditions at different places.
     * @param layer color channel
     * @param x coordinate
     * @param y coordinate
     * @return the value
     */
    public float get(int layer, int x, int y) {
        if (layer<0) {
            layer = 0;
        } else if (layer>=pixel[0][0].length) {
            layer = pixel[0][0].length-1;
        }
        if (x<0) {
            x = 0;
        } else if (x>=width) {
            x = width-1;
        }
        if (y<0) {
            y = 0;
        } else if (y>=height) {
            y = height-1;
        }
        return pixel[x][y][layer];
    }

    /**
     * Sets a component of a pixel.
     * @param layer color channel
     * @param x coordinate
     * @param y coordinate
     * @param value to assign
     */
    public void set(int layer, int x, int y, float value) {
        pixel[x][y][layer] = value;
    }

    /**
     * Changes the color space of the image
     */
    public void toYUV() {
        for (int y=0; y<height; y++) {
            toYUV(y);
        }
    }
    
    /**
     * Change the color space of a given row.
     * @param y row number
     */
    public void toYUV(int y) {
        if (y<0 || y>=getHeight()) {
            return;
        }
        switch (type[y]) {
            case RGB:
                rgb2yuv(y);
                break;
            case HSV:
                hsv2rgb(y);
                rgb2yuv(y);
                break;
        }
    }

    /**
     * Changes the color space.
     */
    public void toRGB() {
        for (int y=0; y<height; y++) {
            toRGB(y);
        }
    }
    
    /**
     * Changes the color space of a given row.
     * @param y row number
     */
    public void toRGB(int y) {
        switch (type[y]) {
            case YUV:
                yuv2rgb(y);
                break;
            case HSV:
                hsv2rgb(y);
                break;
        }
    }
    
    /**
     * Changes the color space.
     */
    public void toHSV() {
        for (int y=0; y<height; y++) {
            toHSV(y);
        }
    }
    
    /**
     * Changes the color space of a given row.
     * @param y row number
     */
    public void toHSV(int y) {
        switch (type[y]) {
            case YUV:
                yuv2rgb(y);
                rgb2hsv(y);
                break;
            case RGB:
                rgb2hsv(y);
                break;
        }
    }

    /**
     * Changes the color space of a given row.
     * @param y row number
     */
    protected void rgb2yuv(int y) {
        for (int x = 0; x < width; x++) {
            float r = pixel[x][y][0];
            float g = pixel[x][y][1];
            float b = pixel[x][y][2];
            pixel[x][y][0] = 0.299f * r + 0.587f * g + 0.114f * b;
            pixel[x][y][1] = (-0.14713f * r - 0.28886f * g + 0.436f * b) / 0.436f / 2 + 0.5f;
            pixel[x][y][2] = (0.615f * r - 0.51498f * g - 0.10001f * b) / 0.615f / 2 + 0.5f;
        }
        type[y] = Type.YUV;
    }

    /**
     * Changes the color space of a given row.
     * @param py row number
     */
    protected void yuv2rgb(int py) {
        for (int x = 0; x < width; x++) {
            float y = pixel[x][py][0];
            float u = (pixel[x][py][1]-0.5f)*2*0.436f;
            float v = (pixel[x][py][2]-0.5f)*2*0.615f;
            pixel[x][py][0] = y + 1.13983f * v;
            pixel[x][py][1] = y - 0.39465f * u - 0.58060f * v;
            pixel[x][py][2] = y + 2.03211f * u;
        }
        type[py] = Type.RGB;
    }
    
    /**
     * Changes the color space of a given row.
     * @param y row number
     */
    protected void rgb2hsv(int y) {
        for (int x=0; x<width; x++) {
            float max = pixel[x][y][0];
            float min = max;
            for (int i=1; i<3; i++) {
                if (max>pixel[x][y][i]) {
                    max = pixel[x][y][i];
                }
                if (min<pixel[x][y][i]) {
                    min = pixel[x][y][i];
                }
            }
            float r = pixel[x][y][0];
            float g = pixel[x][y][1];
            float b = pixel[x][y][2];
            float h = 0;
            if (max!=min) {
                if (max==r) {
                    h = (g-b)/(max-min)/6.0f + 1;
                } else if (max==g) {
                    h = (b-r)/(max-min)/6.0f + 1.0f/3.0f;
                } else if (max==b) {
                    h = (r-g)/(max-min)/6.0f + 2.0f/3.0f;
                }
                while (h>1.0f) {
                    h-=1.0f;
                }
            }
            float s = (max==0) ? 0 : 1.0f-min/max;
            float v = max;
            pixel[x][y][0] = h;
            pixel[x][y][1] = s;
            pixel[x][y][2] = v;
        }
        type[y] = Type.HSV;
    }
    
    /**
     * Changes the color space of a given row.
     * @param y row number
     */
    protected void hsv2rgb(int y) {
        for (int x=0; x<width; x++) {
            float h = pixel[x][y][0];
            float s = pixel[x][y][1];
            float v = pixel[x][y][2];
            int hi = (int)(6.0f*h);
            float f = 6.0f*h - hi;
            float l = v * (1.0f-s);
            float m = v * (1.0f - f*s);
            float n = v * (1.0f - (1-0f-f)*s);
            switch (hi) {
                case 0:
                    pixel[x][y][0] = v;
                    pixel[x][y][1] = n;
                    pixel[x][y][2] = l;
                    break;
                 case 1:
                    pixel[x][y][0] = m;
                    pixel[x][y][1] = v;
                    pixel[x][y][2] = l;
                    break;
                 case 2:
                    pixel[x][y][0] = l;
                    pixel[x][y][1] = v;
                    pixel[x][y][2] = n;
                    break;
                 case 3:
                    pixel[x][y][0] = l;
                    pixel[x][y][1] = m;
                    pixel[x][y][2] = v;
                    break;
                 case 4:
                    pixel[x][y][0] = n;
                    pixel[x][y][1] = l;
                    pixel[x][y][2] = v;
                    break;
                 case 5:
                    pixel[x][y][0] = v;
                    pixel[x][y][1] = l;
                    pixel[x][y][2] = m;
                    break;
            }
        }
        type[y] = Type.RGB;
    }
    
    /**
     * Substract the values to 1.
     */
    public void invert() {
        for (int x=0; x<width; x++) {
            for (int y=0; y<height; y++) {
                for (int l=0; l<3; l++) {
                    pixel[x][y][l] = 1-pixel[x][y][l];
                }
            }
        }
    }
    

    public int getDepth() {
        return 3;
    }


    public float[] getValues(int x, int y) {
        return pixel[x][y];
    }


    public void weightedPaste(float[] source, int from, int x, int y) {
        for (int i=0; i<pixel[x][y].length; i++) {
            pixel[x][y][i] += source[from+i];
        }
       weight[x][y]+=1.0f;
    }
    
    public void normalize() {
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        for (int x=0; x<width; x++) {
            for (int y=0; y<height; y++) {
                for (int l=0; l<pixel[x][y].length; l++) {
                    if (pixel[x][y][l]>max) {
                        max = pixel[x][y][l];
                    }
                    if (pixel[x][y][l]<min) {
                        min = pixel[x][y][l];
                    }
                }
            }
        }
        if (max==min) {
            return;
        }
        for (int x=0; x<width; x++) {
            for (int y=0; y<height; y++) {
                for (int l=0; l<pixel[x][y].length; l++) {
                    pixel[x][y][l] = 2*(pixel[x][y][l]-min) / (max-min)-1;
                }
            }
        }
    }
    
    public void normalizeWeights() {
        for (int x=0; x<width; x++) {
            for (int y=0; y<height; y++) {
                if (weight[x][y]==0.0f) {
                    continue;
                }
                for (int l=0; l<pixel[x][y].length; l++) {
                    pixel[x][y][l] /= weight[x][y];
                }
                weight[x][y] = 1.0f;
            }
        }
    }
    
    public void normalizePatch(int x, int y, int w, int h) {
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;
        for (int px=x; px-x<w; px++) {
            for (int py=y; py-y<h; py++) {
                for (int l=0; l<getDepth(); l++) {
                    float v = get(l, px, py);
                    if (v<min) {
                        min = v;
                    }
                    if (v>max) {
                        max = v;
                    }
                }
            }
        }
        if (min==max) {
            return;
        }
        System.out.print("Normalize patch "+w+"x"+h+", from ["+min+" "+max+"] to ");
        for (int px=x; px-x<w; px++) {
            for (int py=y; py-y<h; py++) {
                for (int l=0; l<getDepth(); l++) {
                    float v = get(l, px, py);
                    set(l, px, py, 2*(v-min)/(max-min)-1);
                }
            }
        }
        
        min = Float.MAX_VALUE;
        max = Float.MIN_VALUE;
        for (int px=x; px-x<w; px++) {
            for (int py=y; py-y<h; py++) {
                for (int l=0; l<getDepth(); l++) {
                    float v = get(l, px, py);
                    if (v<min) {
                        min = v;
                    }
                    if (v>max) {
                        max = v;
                    }
                }
            }
        }
        System.out.println("["+min+" "+max+"]");
    }
}
