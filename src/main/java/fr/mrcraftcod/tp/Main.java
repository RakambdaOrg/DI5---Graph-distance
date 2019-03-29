package fr.mrcraftcod.tp;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import fr.mrcraftcod.tp.model.Graph;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.process.ImageConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;

public class Main{
	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
	private static final int WIDTH_MAX = 600;
	private static final int HEIGHT_MAX = 500;
	private static final int GAP_SIZE = 50;
	
	public static void main(final String[] args) throws IOException, SAXException, ParserConfigurationException{
		LOGGER.info("Starting program version {}", Main.getProgramVersion());
		final CLIParameters parameters = new CLIParameters();
		try{
			JCommander.newBuilder().addObject(parameters).build().parse(args);
		}
		catch(final ParameterException e){
			LOGGER.error("Failed to parse arguments", e);
			e.usage();
			System.exit(1);
		}
		
		final var graphs = new ArrayList<Graph>();
		for(File f : Objects.requireNonNull(new File("torename_GXL").listFiles())){
			graphs.addAll(GXLParser.fromFile(Paths.get(f.toURI())).getGraphs());
		}
		graphs.sort(Comparator.comparing(Graph::getSourcePath));
		
		for(var g1 : graphs){
			for(var g2 : graphs){
				if(!Objects.equals(g1, g2)){
					final var bipartite = g1.getBipartiteCostMatrix(g2);
					final var result = HungarianAlgorithm.hgAlgorithm(bipartite.getAsArray(), "min");
					final var realScore = g1.addEdgeEditionCost(result, g2, g1.getEdgeCostMatrix(g2));
					LOGGER.info("{}~{} vs {}~{} ==> Distance: {}", g1.getSourcePath().getFileName(), g1.getID(), g2.getSourcePath().getFileName(), g2.getID(), result.getLeft());
					
					drawGraphs(g1, g2, result.getRight());
					
					return;
				}
			}
		}
	}
	
	private static void drawGraphs(Graph g1, Graph g2, int[][] matching){
		final var I1 = new ImagePlus(g1.getImagePath().toAbsolutePath().toString());
		final var I2 = new ImagePlus(g2.getImagePath().toAbsolutePath().toString());
		new ImageConverter(I1).convertToRGB();
		new ImageConverter(I2).convertToRGB();
		
		for(var node : g1.getNodes()){
			I1.setColor(Color.BLUE);
			I1.getProcessor().drawOval(node.getAttr("x").map(i -> (Double) i).map(Double::intValue).get() - 2, node.getAttr("y").map(i -> (Double) i).map(Double::intValue).get() - 2, 4, 4);
		}
		for(var edge : g1.getEdges()){
			final var node1 = g1.getNodeByID(edge.getFrom()).orElseThrow();
			final var node2 = g1.getNodeByID(edge.getTo()).orElseThrow();
			I1.setColor(Color.YELLOW);
			I1.getProcessor().drawLine(node1.getAttr("x").map(i -> (Double) i).map(Double::intValue).get(), node1.getAttr("y").map(i -> (Double) i).map(Double::intValue).get(), node2.getAttr("x").map(i -> (Double) i).map(Double::intValue).get(), node2.getAttr("y").map(i -> (Double) i).map(Double::intValue).get());
		}
		
		for(var node : g2.getNodes()){
			I2.setColor(Color.BLUE);
			I2.getProcessor().drawOval(node.getAttr("x").map(i -> (Double) i).map(Double::intValue).get() - 2, node.getAttr("y").map(i -> (Double) i).map(Double::intValue).get() - 2, 4, 4);
		}
		for(var edge : g2.getEdges()){
			final var node1 = g2.getNodeByID(edge.getFrom()).orElseThrow();
			final var node2 = g2.getNodeByID(edge.getTo()).orElseThrow();
			I2.setColor(Color.YELLOW);
			I2.getProcessor().drawLine(node1.getAttr("x").map(i -> (Double) i).map(Double::intValue).get(), node1.getAttr("y").map(i -> (Double) i).map(Double::intValue).get(), node2.getAttr("x").map(i -> (Double) i).map(Double::intValue).get(), node2.getAttr("y").map(i -> (Double) i).map(Double::intValue).get());
		}
		
		final var I3 = ConcatenateImage(I1, I2, GAP_SIZE);
		final var xOffset = I1.getWidth() + GAP_SIZE;
		
		for(final var node : g1.getNodes()){
			final var nodeIndex = g1.getNodeIndex(node.getID()).orElseThrow();
			final var assign = Stream.of(matching).filter(m -> m[0] == nodeIndex).findAny().orElseThrow();
			
			if(assign[1] < g2.getNodeCount()){
				final var node2 = g2.getNodeAt(assign[1]).orElseThrow();
				
				I3.setColor(Color.LIGHT_GRAY);
				I3.getProcessor().drawLine(node.getAttr("x").map(i -> (Double) i).map(Double::intValue).get(), node.getAttr("y").map(i -> (Double) i).map(Double::intValue).get(), node2.getAttr("x").map(i -> (Double) i).map(Double::intValue).get() + xOffset, node2.getAttr("y").map(i -> (Double) i).map(Double::intValue).get());
			}
		}
		
		// correctly matched vertices
		//I3.setColor(Color.LIGHT_GRAY);
		//I3.getProcessor().drawLine(x1int, y1int, x2int + i1.getWidth() + gap, y2int);
		// mismatched vertices
		//I3.setColor(Color.red);
		//I3.getProcessor().setLineWidth(10);
		//I3.getProcessor().drawLine(x1int, y1int, x2int + i1.getWidth() + gap, y2int);
		
		I3.show();
	}
	
	private static ImagePlus ConcatenateImage(ImagePlus i1, ImagePlus i2, int gap){
		var newWidth = gap + WIDTH_MAX * 2;
		final var res = NewImage.createRGBImage("new image", newWidth, HEIGHT_MAX, 1, NewImage.FILL_BLACK);
		final var ip = res.getProcessor();
		for(int x = 0; x < i1.getWidth(); x++){
			for(int y = 0; y < i1.getHeight(); y++){
				int p = i1.getProcessor().getPixel(x, y);
				ip.set(x, y, p);
			}
		}
		for(int x = 0; x < i2.getWidth(); x++){
			for(int y = 0; y < i2.getHeight(); y++){
				int p = i2.getProcessor().getPixel(x, y);
				ip.set(x + i1.getWidth() + gap, y, p);
			}
		}
		res.setProcessor("house", ip);
		//res.show();
		return res;
	}
	
	public static String getProgramVersion(){
		final Properties properties = new Properties();
		try{
			properties.load(Main.class.getResource("/version.properties").openStream());
		}
		catch(final IOException e){
			LOGGER.warn("Error reading version jsonConfigFile", e);
		}
		return properties.getProperty("simulator.version", "Unknown");
	}
}
