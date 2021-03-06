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

package gr.demokritos.iit.sproduce.generator;

import org.apache.commons.cli.*;

import org.jzy3d.chart.Chart;
import org.jzy3d.chart.ChartLauncher;
import org.jzy3d.chart.factories.AWTChartComponentFactory;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.colors.colormaps.ColorMapRainbow;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.Point;
import org.jzy3d.plot3d.primitives.Polygon;
import org.jzy3d.plot3d.primitives.Shape;
import org.jzy3d.plot3d.rendering.canvas.Quality;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Application which creates a file with a list of real numbers
 * considered as surface heights. The resulting surface is either
 * Gaussian isotropic or non-isotropic.
 * <br>The concept of this application is to simulate the production
 * of a square piece of material with characteristics
 * visible on the nano scale.</p>
 *
 * <p>Each side has a certain length (<i>-rL</i>)
 * and consists of a number (<i>-N</i>) of points to be provided.
 * Rms height (<i>-h</i>) and correlation length on x axis (<i>-clx</i>) are also needed.
 * The provision of correlation length on y axis (<i>-cly</i>) determinates
 * whether the surface would be isotropic or non-isotropic.</p>
 * Input is passed either through an input file (<i>-in</i>), which may contain multiple surfaces'
 * parameters, or through standard input for one surface at a time.
 * Output can be forwarded through file (<i>-out</i>), or standard output, in which case
 * a 3D image is produced.
 *
 * @author  Antonia Tsili
 * @version 1.0
 * @since   2018-08
 */
public class ProduceSurface {

    /**
     * @param argv          N,input_file or (length,rms_height,clx,cly*),output_file*
     * @throws Exception    Related to input parameters
     *
     * <p><i>Asterisk (*) denotes optional argument.</i>
     * <br><i>Input file(.cvs) format:
     * <ul>
     *      <li>",Rms,clx,cly,Skewness,Kurtosis,Area" as a header</li>
     *      <li>ID,Rms,clx,cly,Skewness,Kurtosis,Area per line</li>
     * </ul>
     * </i></p>
     */
    public static void main(String[] argv) throws Exception{

        Options options = new Options();

        Option N = new Option("N", "npoints", true, "number (power of 2) of surface points along square side");
        N.setRequired(true);
        options.addOption(N);

        Option input = new Option("in", "input", true, "input file");
        input.setRequired(false);
        options.addOption(input);

        Option rL = new Option("rL", "length", true, "length of surface along square side");
        rL.setRequired(false);
        options.addOption(rL);

        Option rms = new Option("h", "rms_height", true, "rms height");
        rms.setRequired(false);
        options.addOption(rms);

        Option clx = new Option("clx", "clx", true, "correlation length x axis");
        clx.setRequired(false);
        options.addOption(clx);

        Option cly = new Option("cly", "cly", true, "correlation length y axis");
        cly.setRequired(false);
        options.addOption(cly);

        Option output = new Option("out", "output", true, "output file");
        output.setRequired(false);
        options.addOption(output);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, argv);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
        }

        String out_filename= "";
        String in_filename = "";
        String cvsSplitBy  = ",";
        int y_flag 	       = 0;
        int out_flag       = 0;
        int in_flag        = 0;
        double[] args_     = new double[5];
        args_[0] = Double.parseDouble((String) cmd.getOptionValue("N"));

        // check if the input file name argument has been passed
        if( !cmd.hasOption( "in" ) ) { // if not, we use standard input

            System.out.println("No input file detected. Using command line...");

            // check if needed remain arguments are added
            if( !( cmd.hasOption( "rL" ) && cmd.hasOption( "h" ) && cmd.hasOption( "clx" )) ) {
                System.out.println("Surface length, rms height and correlation lentgh x axis are necessary");
                System.exit(1);
            } else {
                args_[1] = Double.parseDouble((String) cmd.getOptionValue("rL"));
                args_[2] = Double.parseDouble((String) cmd.getOptionValue("h"));
                args_[3] = Double.parseDouble((String) cmd.getOptionValue("clx"));
            }
        } else {
            in_filename = cmd.getOptionValue("input");
            in_flag = 1;
        }

        if( cmd.hasOption( "cly" )) { // check if cly argument is passed
            System.out.println("Found cly argument. Surface is non-isotropic.");

            args_[4] = Double.parseDouble((String) cmd.getOptionValue("cly"));
            y_flag = 1;
        }

            if( cmd.hasOption( "out" ) ){
                out_filename = cmd.getOptionValue("out");
                out_flag = 1;

                // check if file exists
                File f = new File(out_filename);
                // erase content if exists
                if(f.exists() && !f.isDirectory()) {
                    FileWriter writer = new FileWriter(out_filename);
                    writer.write("");
                    writer.close();
                }
            }

        // read from standard input
        if( in_flag==0 ) {
            RandomGaussSurfaceGenerator RG = produce(args_,y_flag,out_flag,out_filename);
            plot_surface(RG);

//            CSVReader reader = new CSVReader();
//            reader.test("stdin_results.csv");

        // read from csv file with multiple surface parameters
        } else {
            BufferedReader reader = null;
            String line       = "";
            y_flag            = 0;
            try {

                reader = new BufferedReader(new FileReader(in_filename));
                line = reader.readLine(); // get first line with names of parameters
                String[] all_params = line.split(cvsSplitBy);
                for (int i=0; i<all_params.length ; i++) {
                    if( all_params[i].equals("cly") )
                        y_flag = 1;
                }
                while ((line = reader.readLine()) != null) {
                    // use comma as separator
                    all_params = line.split(cvsSplitBy);

                    args_[1] = Math.sqrt(Double.parseDouble(all_params[6]));
                    args_[2] = Double.parseDouble(all_params[1]);
                    args_[3] = Double.parseDouble(all_params[2]);
                    if( y_flag==1 ) args_[4] = Double.parseDouble(all_params[3]);

                    RandomGaussSurfaceGenerator RG = produce(args_,y_flag,out_flag,out_filename);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }

    }

    /**
     * <p>This function creates a surface generator instance,
     * which is loaded with all the needed and provided parameters.
     * <br>The resulting surface is a member of the generator class.</p>
     *
     * @param args_         Parameters read from input
     * @param y_flag        Determines whether surface will be (non-)isotropic
     * @param out_flag      Determines whether output will be printed to file
     * @param out_filename  Name of output file
     * @return              Instance of surface generator
     * @throws ImError      If Fourier transformation did not succeed
     * @throws IOException  If there was an error creating or writing to file
     */
    static protected RandomGaussSurfaceGenerator produce(double[] args_, int y_flag, int out_flag, String out_filename) throws ImError, IOException {
        RandomGaussSurfaceGenerator RG;
        if( y_flag==0 )
            RG = new RandomGaussSurfaceGenerator(args_); // isotropic
        else
            RG = new RandomGaussSurfaceGenerator(args_,args_[4]); // non-isotropic,last argument is cly

        if( out_flag==0 ){ // standard output
            RG.printArray(RG.Surf);
        } else {
            try{
                FileWriter writer = new FileWriter(out_filename,true);
                RG.printArray(writer,RG.Surf);
            } catch (IOException ex){
                System.out.println("There was a problem creating/writing to the file");
                ex.printStackTrace();
            }
        }
        return RG;
    }

    /**
     * <p>Used only with input provided through standard input for one surface at a time.
     * <br>The function creates a 3D image with gradient colours which show height differences.
     * <br>It provokes a pop-up window with the correspondent figure, that can also be
     * turned at will using the cursor. The produced image is saved as a file.</p>
     *
     * @param RG            Instance of surface generator class
     * @throws IOException
     * @see org.jzy3d.chart
     */
    static protected void plot_surface(RandomGaussSurfaceGenerator RG) throws IOException {
        double[][] distDataProp = RG.Surf;

        // Build a polygon list
        List<Polygon> polygons = new ArrayList<Polygon>();
        for(int i = 0; i < distDataProp.length -1; i++){
            for(int j = 0; j < distDataProp[i].length -1; j++){
                Polygon polygon = new Polygon();
                polygon.add(new Point( new Coord3d(i, j, distDataProp[i][j]) ));
                polygon.add(new Point( new Coord3d(i, j+1, distDataProp[i][j+1]) ));
                polygon.add(new Point( new Coord3d(i+1, j+1, distDataProp[i+1][j+1]) ));
                polygon.add(new Point( new Coord3d(i+1, j, distDataProp[i+1][j]) ));
                polygons.add(polygon);
            }
        }

        // Creates the 3d object
        Shape surface = new Shape(polygons);
        surface.setColorMapper(new ColorMapper(new ColorMapRainbow(), surface.getBounds().getZmin(), surface.getBounds().getZmax(), new org.jzy3d.colors.Color(1,1,1,1f)));
        surface.setWireframeDisplayed(false);

        Chart chart = new AWTChartComponentFactory().newChart(Quality.Advanced, "awt");
        chart.getScene().getGraph().add(surface);
        ChartLauncher.openChart(chart);
        File image = new File("surface.png");
        chart.screenshot(image);
    }
}

