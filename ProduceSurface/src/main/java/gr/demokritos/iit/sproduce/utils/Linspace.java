/**
 * Copyright 2018 Antonia Tsili NCSR Demokritos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.demokritos.iit.sproduce.utils;

import java.lang.Math;
import java.math.BigDecimal;


/**
 * <p>Generates n points. The spacing between the points is (arg2-arg1)/(n-1).</p>
 */
public class Linspace {

    /**
     * Number of points
     */
    private int total;
    /**
     * End of spacing
     */
    private final double end;
    /**
     * Start of spacing
     */
    private final double start;

    /**
     * <p>Initialization</p>
     *
     * @param start         Start of total spacing
     * @param end           End of total spacing
     * @param totalCount    Number of points
     */
    public Linspace(double start, double end, int totalCount) {
        this.total   = (int)Math.floor( (double)totalCount )-1;
        if( start>end ){ // check for wrong sequence of arguments
            this.end   = start;
            this.start = end;
        } else {
            this.end   = end;
            this.start = start;
        }

    }

    /**
     * <p>Operation</p>
     *
     * @return Array of real numbers as points
     */
    public double[] op() {
        // check intermediate value for appropriate treatment
        // throws exception at overflow
        try{
            BigDecimal.valueOf(Math.multiplyExact((long)(end-start),(long)total-1));
            double[] res = new double[total+1];
            if( end*start<0 ){
                for (int i=0 ; i<=total ; i++) {
                    res[i] = start + (end/total)*i - (start/total)*i;
                }
            } else {
                for (int i=0; i<=total ; i++) {
                    res[i] = start + i*( (end-start)/total );
                }
            }
            return res;
            // in case of overflow
        } catch (ArithmeticException e) {
            double[] res = new double[total+1];
            for (int i=0; i<=total ; i++) {
                res[i] = start + i*( (end-start)/total );
            }
            return res;
        }

    }

}