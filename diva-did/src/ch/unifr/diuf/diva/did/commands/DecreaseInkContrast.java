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

import ch.unifr.diuf.diva.did.Image;
import ch.unifr.diuf.diva.did.Script;
import java.io.IOException;
import org.jdom2.Element;
import org.jdom2.JDOMException;

/**
 *
 * @author Mathias Seuret
 */
public class DecreaseInkContrast extends AbstractCommand {

    public DecreaseInkContrast(Script script) {
        super(script);
    }

    @Override
    public float execute(Element task) throws IOException, JDOMException, InterruptedException {
        String refName = task.getAttributeValue("ref");
        if (refName == null) {
            throw new IllegalArgumentException(
                    "\n" + commandName + ": ref is required"
            );
        }
        refName = script.preprocess(refName);

        if (!script.getImages().containsKey(refName)) {
            throw new IllegalArgumentException(
                    "\n" + commandName + ": cannot find ref image " + refName
            );
        }
        Image img = script.getImages().get(refName);

        float strength   = this.getChildFloat(task, "strength");
        float threshold  = this.getChildFloat(task, "threshold");
        float randomness = this.getChildFloat(task, "random");
        
        int heightMapSize = 2;
        while (heightMapSize<img.getWidth() && heightMapSize<img.getHeight()) {
            heightMapSize *= 2;
        }
        Heightmap heightmap = new Heightmap(heightMapSize);
        heightmap.diamondSquare();

        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                float grey = 0;
                for (int c = 0; c < img.getDepth(); c++) {
                    grey += img.get(c, x, y);
                }
                grey /= img.getDepth();
                if (grey > threshold) {
                    continue;
                }
                float dist = threshold - grey;
                float dist2 = dist * (float)Math.exp(-dist * dist * strength)
                              * (1-randomness*heightmap.get(x,y));
                float targ = threshold - dist2;
                float ratio = targ / grey;
                for (int c = 0; c < img.getDepth(); c++) {
                    float v = ratio * img.get(c, x, y);
                    img.set(c, x, y, v);
                }
            }
        }

        return 0;
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
