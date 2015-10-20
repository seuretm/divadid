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
package ch.unifr.diuf.diva.did.commands;

import ch.unifr.diuf.diva.did.GradientMap;
import ch.unifr.diuf.diva.did.Image;
import ch.unifr.diuf.diva.did.Script;
import java.io.IOException;
import org.jdom2.Element;

/**
 *
 * @author Mathias Seuret
 */
public class DecreaseMaxContrast extends AbstractGradientManipulation {

    public DecreaseMaxContrast(Script script) {
        super(script);
    }

    @Override
    public void modifyGradient(Element task, GradientMap[] gradients, Image image) throws IOException {
        
        float strength = this.getChildFloat(task, "strength");
        
        int heightMapSize = 2;
        while (heightMapSize<image.getWidth() && heightMapSize<image.getHeight()) {
            heightMapSize *= 2;
        }
        Heightmap heightmap = new Heightmap(heightMapSize);
        heightmap.diamondSquare();
        heightmap.square();
        
        Image img = new Image(heightmap.size, heightmap.size);
        for (int x=0; x<heightmap.size; x++) {
            for (int y=0; y<heightmap.size; y++) {
                img.set(0, x, y, heightmap.get(x,y));
                img.set(1, x, y, heightmap.get(x,y));
                img.set(2, x, y, heightmap.get(x,y));
            }
        }
        img.write("heightmap.jpg", "jpg");
        
        for (GradientMap g : gradients) {
            for (int dir=0; dir<2; dir++) {
                float[][] v = (dir==0) ? g.getGX() : g.getGY();
                for (int x=0; x<g.getWidth(); x++) {
                    for (int y=0; y<g.getHeight(); y++) {
                        //v[x][y] *= 1-strength*heightmap.get(x, y);
                        v[x][y] *= Math.exp(-strength*heightmap.get(x,y));
                    }
                }
            }
        }
        
    }
    
    private class Heightmap {

        int size;
        float[][] height;

        Heightmap(int size) {
            this.size = size + 1;
            System.out.println(this.size);
            height = new float[this.size][this.size];
            flat();
        }

        int size() {
            return size;
        }

        final void flat() {
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    height[x][y] = 0.0f;
                }
            }
        }

        void normalize() {
            float min = Float.POSITIVE_INFINITY;
            float max = Float.NEGATIVE_INFINITY;
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    if (height[x][y] < min) {
                        min = height[x][y];
                    }
                    if (height[x][y] > max) {
                        max = height[x][y];
                    }
                }
            }
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    height[x][y] = (height[x][y] - min) / (max - min);
                }
            }
        }

        void noise(double dh) {
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    height[x][y] += Math.random() * dh * Math.pow(-1, (int) (1 + Math.random() * 2));
                }
            }
            normalize();
        }

        void square() {
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    height[x][y] *= Math.abs(height[x][y]);
                }
            }
            normalize();
        }

        void smooth() {
            float[][] nHeight = new float[size][size];
            int s = 4;
            float factor = 3.9f;
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    float sum = 0.0f;
                    nHeight[x][y] = 0.0f;
                    for (int dx = -s; dx <= s; dx++) {
                        for (int dy = -s; dy <= s; dy++) {
                            if (x + dx >= 0 && x + dx < size && y + dy >= 0 && y + dy < size) {
                                float ratio = (float)Math.exp(-Math.pow(dx * dx + dy * dy, 2) / factor);
                                nHeight[x][y] += ratio * height[x + dx][y + dy];
                                sum += ratio;
                            }
                        }
                    }
                    nHeight[x][y] /= sum;
                }
            }
            height = nHeight;
            normalize();
        }

        void diamondSquare() {
            int stepLength = size - 1;
            int s = size - 1;
            float diffHeight = 1.5f;
            height[0][0] = (float)Math.random();
            height[0][size - 1] = (float)Math.random();
            height[size - 1][0] = (float)Math.random();
            height[size - 1][size - 1] = (float)Math.random();
            do {
                for (int x = 0; x <= s; x += 2 * stepLength) {
                    for (int y = 0; y <= s; y += 2 * stepLength) {
                        // square step
                        int cx = x + stepLength;
                        int cy = y + stepLength;
                        if (x + 2 * stepLength <= s && y + 2 * stepLength <= s) {
                            height[cx][cy] = 0.25f * (height[x][y] + height[x + 2 * stepLength][y + 2 * stepLength]
                                    + height[x + 2 * stepLength][y] + height[x][y + 2 * stepLength])
                                    + (float)(0.5 - Math.random()) * diffHeight;
                        }
                        // diamond step
                        if (x + 2 * stepLength <= s) {
                            height[cx][y] = 0.5f * (height[x][y] + height[x + 2 * stepLength][y])
                                    + (float)(0.5 - Math.random()) * diffHeight;
                        }
                        if (y + 2 * stepLength <= s) {
                            height[x][cy] = 0.5f * (height[x][y] + height[x][y + 2 * stepLength])
                                    + (float)(0.5 - Math.random()) * diffHeight;
                        }
                    }
                }
                stepLength /= 2.0;
                diffHeight *= 0.97;
            } while (stepLength > 0);
            normalize();
        }

        float get(int x, int y) {
            return height[x][y];
        }
    }
    
}
