package sssj;

import static sssj.util.Commons.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import sssj.index.Index;
import sssj.index.IndexStatistics;
import sssj.index.IndexType;
import sssj.index.streaming.StreamingINVIndex;
import sssj.index.streaming.StreamingL2APIndex;
import sssj.index.streaming.StreamingL2Index;
import sssj.io.Format;
import sssj.io.Vector;
import sssj.io.VectorStream;
import sssj.io.VectorStreamFactory;
import sssj.time.Timeline.Sequential;

import com.github.gdfm.shobaidogu.ProgressTracker;
import com.google.common.base.Joiner;

/**
 * Streaming method. Fully incremental, online (zero latency). Keeps the index pruned via time filtering.
 * Efficient pruning supported via circular buffers. Supports three types of index (INV, L2AP, L2).
 */
public class Streaming {
  private static final String ALGO = "Streaming";
  private static final Logger log = LoggerFactory.getLogger(Streaming.class);
  
  static Map<Integer, Integer> gabarito; 
  
  
  static  int true_positivo=0;
  static int false_positivo=0;
  static int false_negativo=0;
  
  
  public static void main(String[] args) throws Exception {
    ArgumentParser parser = ArgumentParsers.newArgumentParser(ALGO).description("SSSJ in " + ALGO + " mode.")
        .defaultHelp(true);
    parser.addArgument("-t", "--theta").metavar("theta").type(Double.class).choices(Arguments.range(0.0, 1.0))
        .setDefault(DEFAULT_THETA).help("similarity threshold");
    parser.addArgument("-l", "--lambda").metavar("lambda").type(Double.class)
        .choices(Arguments.range(0.0, Double.MAX_VALUE)).setDefault(DEFAULT_LAMBDA).help("forgetting factor");
    parser.addArgument("-r", "--report").metavar("period").type(Integer.class).setDefault(DEFAULT_REPORT_PERIOD)
        .help("progress report period");
    parser.addArgument("-i", "--index").type(IndexType.class)
        .choices(IndexType.INV, IndexType.L2AP, IndexType.L2).setDefault(IndexType.L2)
        .help("type of indexing");
    parser.addArgument("-f", "--format").type(Format.class).choices(Format.values()).setDefault(Format.BINARY)
        .help("input format");
    parser.addArgument("input").metavar("file")
        .type(Arguments.fileType().verifyExists().verifyIsFile().verifyCanRead()).help("input file");
    for (int i = 0; i < args.length; i++) {
    	System.out.println(args[i]);
	}
    
//    args[0]="data/dirty_1000_100_SVM";
  args[1]="0.7";
  //args[2]="0.2";
//  args[3]="-i   INV ";
  args[3]="0.01";
  args[8]="data/scholar_SVM";
    
    
    Namespace opts = parser.parseArgsOrFail(args);

    final double theta = opts.get("theta");
    final double lambda = opts.get("lambda");
    final int reportPeriod = opts.getInt("report");
    final IndexType idxType = opts.<IndexType>get("index");
    final Format fmt = opts.<Format>get("format");
    final File file = opts.<File>get("input");
    final VectorStream stream = VectorStreamFactory.getVectorStream(file, fmt, new Sequential());
    final long numVectors = stream.numVectors();
    final ProgressTracker tracker = new ProgressTracker(numVectors, reportPeriod);

    gabarito= load_gabarito(opts.get("input"));
    final String header = String.format(ALGO + " [d=%s, t=%f, l=%f, i=%s]", file.getName(), theta, lambda,
        idxType.toString());
    System.out.println(header);
    log.info(header);
    final long start = System.currentTimeMillis();
    final IndexStatistics stats = compute(stream, theta, lambda, idxType, tracker);
    final long elapsed = System.currentTimeMillis() - start;
    final String csvLine = Joiner.on(",").join(ALGO, file.getName(), theta, lambda, idxType.toString(), elapsed,
        stats.numPostingEntries(), stats.numCandidates(), stats.numSimilarities(), stats.numMatches());
    System.out.println(csvLine);
    log.info(String.format(ALGO + " [d=%s, t=%f, l=%f, i=%s, time=%d]", file.getName(), theta, lambda,
        idxType.toString(), elapsed));
  }

  public static Map<Integer, Integer> load_gabarito(File file){
	  Map<Integer, Integer> gab = new HashMap<Integer, Integer>();
	  
	  BufferedReader br;
	try {
		br = new BufferedReader(new FileReader(file.getParentFile()+"/"+file.getName().replace("_SVM", "")));
	
	  
	      StringBuilder sb = new StringBuilder();
	      String line = br.readLine();
	      int num=0;
	      while (line != null) {
	    	  gab.put(Integer.parseInt(line.split(";")[0]), Integer.parseInt(line.split(";")[1]));
	          //System.out.println(num + "  "+ line.split(",")[0]);
	          line = br.readLine();
	          
	          num++;
	      }
	
	  }  catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	 return gab; 
	  
  }
  
  public static IndexStatistics compute(Iterable<Vector> stream, double theta, double lambda, IndexType type,
      ProgressTracker tracker) {
    Index index = null;
    switch (type) {
    case INV:
      index = new StreamingINVIndex(theta, lambda);
      break;
    case L2AP:
      index = new StreamingL2APIndex(theta, lambda);
      break;
    case L2:
      index = new StreamingL2Index(theta, lambda);
      break;
    default:
      throw new RuntimeException("Unsupported index type");
    }
    assert (index != null);

    Mean avgSize = new Mean();
    for (Vector v : stream) {
      if (tracker != null)
        tracker.progress();
      //System.out.println(v.toString());
      Map<Long, Double> results = index.queryWith(v, true);
      IndexStatistics stats = index.stats();
      avgSize.increment(stats.size());
     // if (!results.isEmpty())
     //   System.out.println(v.timestamp() + " ~ " + formatMap(results));
      if (!results.isEmpty())
    	  valida_res((v.timestamp()), results);
 
    }
    

    for (Entry<Integer, Integer> entry : gabarito.entrySet()){
//    	if(entry.getValue().contains("dup")){
//    		//System.out.println(entry.getValue());
//    		false_negativo++;
//    	}
    	
    }
    Double p=(true_positivo/(double)(true_positivo+false_positivo));
    Double r=(true_positivo/(double)(true_positivo+false_negativo));
    System.out.println("\n\nprecisão " + p);
    System.out.println("Recall " + r);
    System.out.println("positivos " + true_positivo + "  fase " + false_positivo +" false negative " + false_negativo);
    System.out.println("F1 " + (2*p*r)/(p+r));
    
    final StringBuilder sb = new StringBuilder();
    sb.append("Index Statistics:\n");
    sb.append(String.format("Average index size           = %.3f\n", avgSize.getResult()));
    sb.append(String.format("Total number of entries      = %d\n", index.stats().numPostingEntries()));
    sb.append(String.format("Total number of candidates   = %d\n", index.stats().numCandidates()));
    sb.append(String.format("Total number of similarities = %d\n", index.stats().numSimilarities()));
    sb.append(String.format("Total number of matches      = %d", index.stats().numMatches()));
    log.info(sb.toString());
    return index.stats();
  }
  
  
  private static void valida_res(long l, Map<Long, Double> res){
	  for (Entry<Long, Double> row : res.entrySet()) {
	    	System.out.println(row.getKey().intValue() + " ~ " + (l));
	    	//Double values = row.getValue();
	    	System.out.println("ow.getKey() " + row.getKey());
	    	Integer recB=gabarito.get(row.getKey().intValue());
	    	Integer recA=gabarito.get((int)l);
	    	//for (Entry<Long, Double> entry : values.entrySet()){
	    	//System.out.println(recA +"  "+ recB);
	    	if(recA!=null && recA== row.getKey().intValue()){
	    		System.out.println("achouuuu " );
	    		System.out.println("key value" +row.getKey().intValue() + " ~ " + (l));
	    	}
	    	if(recB!=null && recB== row.getKey().intValue()){
	    		System.out.println("achouuuu " );
	    		System.out.println("key value" +row.getKey().intValue() + " ~ " + (l));
	    	}
//	    		if(recB==null){
//	    			//System.out.println("erro validação gabarito--->"+entry.getKey());
//	    			continue;
//	    		}
//	    		if(!recA.contains("-"))
//	    			continue;
//	    		if(!recB.contains("-"))
//	    			continue;
//	    		String idA = recA.split("-")[1];
//	    		String idB = recB.split("-")[1];
//	    		if(recA.contains("org") && recB.contains("org")){
//	    			false_positivo++;
//	    			continue;
//	    		}
	    			
//	    		if(idA.equals(idB) && (recA.contains("dup") || recB.contains("dup"))){
//	    			
//	    			if(recA.contains("dup") && recB.contains("org")){
//	    				gabarito.put((int) (long)l,recA.replace("dup", ""));
//	    				true_positivo++;
//		    			//System.out.println(recA +" --- "+ recB);
//	    			}else
//	    				if(recA.contains("org")){
//	    					gabarito.put(row.getKey().intValue(),recB.replace("dup-", ""));
//	    					true_positivo++;
//	    	    			//System.out.println(recA +" --- "+ recB);
//	    				}
//	    			
//	    			//map.put(key, map.get(key) + 1);
//	    			
//	    		}
//	    		else
//	    			
//	    				false_positivo++;
//	    				
	    	}
	    
  }
}
