package fr.mrcraftcod.tp.model;

import org.apache.commons.lang3.builder.ToStringBuilder;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import static fr.mrcraftcod.tp.model.EdgeMode.UNDIRECTED;

/**
 * Created by mrcraftcod (MrCraftCod - zerderr@gmail.com) on 2019-03-22.
 *
 * @author Thomas Couchoud
 * @since 2019-03-22
 */
public class Graph{
	private final int ID;
	private final ArrayList<Node> nodes;
	private final ArrayList<Edge> edges;
	private EdgeMode edgeMode;
	
	public Graph(int id){
		this.ID = id;
		this.edgeMode = UNDIRECTED;
		this.nodes = new ArrayList<>();
		this.edges = new ArrayList<>();
	}
	
	public void addNode(Node node){
		this.nodes.add(node);
	}
	
	public void addEdge(Edge edge){
		this.edges.add(edge);
	}
	
	@Override
	public String toString(){
		return new ToStringBuilder(this).append("ID", ID).append("nodes", nodes).append("edges", edges).append("edgeMode", edgeMode).toString();
	}
	
	public Optional<Node> getNodeByID(int id){
		return this.getNodes().stream().filter(n -> Objects.equals(n.getID(), id)).findAny();
	}
	
	public ArrayList<Node> getNodes(){
		return this.nodes;
	}
	
	public Optional<Edge> getEdge(int from, int to){
		return this.getEdges().stream().filter(e -> (Objects.equals(e.getFrom(), from) || Objects.equals(e.getTo(), to)) || (Objects.equals(this.getEdgeMode(), UNDIRECTED) && (Objects.equals(e.getFrom(), to) || Objects.equals(e.getTo(), from)))).findAny();
	}
	
	public ArrayList<Edge> getEdges(){
		return this.edges;
	}
	
	private EdgeMode getEdgeMode(){
		return this.edgeMode;
	}
	
	public void setEdgeMode(EdgeMode edgemode){
		this.edgeMode = edgemode;
	}
	
	public Optional<Edge> getEdgeAt(int pos){
		if(pos < 0 || pos >= this.getEdgeCount()){
			return Optional.empty();
		}
		return Optional.of(this.getEdges().get(pos));
	}
	
	private int getEdgeCount(){
		return this.getEdges().size();
	}
	
	public Matrix2D getEdgeCostMatrix(final Graph graph){
		final var edgeCost = 1D; //This is a predefined param
		final var alpha = 0.5D;

		final var matrix = new Matrix2D(this.getEdgeCount() + 1, graph.getEdgeCount() + 1);

		for(int i = 0; i < this.getEdgeCount() + 1; i++){
			for(int j = 0; j < graph.getEdgeCount() + 1; j++){
				final var cost = 0D;

				if(!Objects.equals(i, this.getEdgeCount()) || !Objects.equals(j, graph.getEdgeCount())){
					if(Objects.equals(i, this.getEdgeCount()) || Objects.equals(j, graph.getEdgeCount())){
						cost = (1 - alpha) * edgeCost;
					}
				}
			}
		}
		
		return matrix;
	}
	
	public Matrix2D getNodeCostMatrix(final Graph graph){
		final var nodeCost = 100000D; //This is a predefined param
		final var alpha = 0.5D;
		final var matrix = new Matrix2D(this.getNodeCount() + 1, graph.getNodeCount() + 1);
		
		for(int i = 0; i < this.getNodeCount() + 1; i++){
			for(int j = 0; j < graph.getNodeCount() + 1; j++){
				double cost = 0D;
				
				if(!Objects.equals(i, this.getNodeCount()) || !Objects.equals(j, graph.getNodeCount())){
					if(Objects.equals(i, this.getNodeCount()) || Objects.equals(j, graph.getNodeCount())){
						cost = alpha * nodeCost;
					}
					else{
						final var ni = this.getNodeAt(i).orElseThrow(IndexOutOfBoundsException::new);
						final var nj = graph.getNodeAt(j).orElseThrow(IndexOutOfBoundsException::new);
						
						cost = alpha * ni.getAttributes().entrySet().stream().filter(e -> !Objects.equals("x", e.getKey()) && !Objects.equals("y", e.getKey())).mapToDouble(e -> nj.getAttr(e.getKey()).map(v -> computeDistance(v, e.getValue())).orElse(0D)).sum();
					}
				}
				matrix.put(i, j, cost);
			}
		}
		
		return matrix;
	}
	
	private int getNodeCount(){
		return this.getNodes().size();
	}
	
	public Optional<Node> getNodeAt(int pos){
		if(pos < 0 || pos >= this.getNodeCount()){
			return Optional.empty();
		}
		return Optional.of(this.getNodes().get(pos));
	}
	
	private double computeDistance(Object o1, Object o2){
		return Math.abs((Double) o1 - (Double) o2);
	}
}
