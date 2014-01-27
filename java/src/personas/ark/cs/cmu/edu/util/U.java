package personas.ark.cs.cmu.edu.util;



import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

//import org.codehaus.jackson.JsonNode;
//import org.codehaus.jackson.JsonProcessingException;
//import org.codehaus.jackson.map.ObjectMapper;

//import edu.stanford.nlp.util.Pair;
//import edu.stanford.nlp.util.StringUtils;

/** misc utilities **/
public class U {
//	public static JsonNode readJson(String jsonStr) throws JsonProcessingException, IOException {
//		ObjectMapper mapper = new ObjectMapper();
//		return mapper.readTree(jsonStr);
//	}
//	/** static creator so don't have to specify types **/
//	public static <S,T> Pair<S,T> pair(S o1, T o2) {
//		return new Pair(o1, o2);
//	}
	
	public static void p(Object x) { System.out.println(x); }
	public static void p(String[] x) { p(Arrays.toString(x)); }
	public static void p(double[] x) { p(Arrays.toString(x)); }
	public static void p(int[] x) { p(Arrays.toString(x)); }
	public static void p(double[][] x) {
		System.out.printf("(%s x %s) [\n", x.length, x[0].length);
		for (double[] row : x) {
			System.out.printf(" ");
			p(Arrays.toString(row));
		}
		p("]");
	}
//	public static String sp(double[] x) {
//		ArrayList<String> parts = new ArrayList<String>();
//		for (int i=0; i < x.length; i++)
//			parts.add(String.format("%.2g", x[i]));
//		return "[" + StringUtils.join(parts) + "]";
//	}
	public static void p(String x) { System.out.println(x); }
	
	
	public static void pf(String pat) {  System.out.printf(pat);  }

	public static <A> void pf(String pat, A a0) {  System.out.printf(pat, a0);  }
	public static <A> String sf(String pat, A a0) {  return String.format(pat, a0);  }
	public static <A,B> void pf(String pat, A a0, B a1) {  System.out.printf(pat, a0, a1);  }
	public static <A,B> String sf(String pat, A a0, B a1) {  return String.format(pat, a0, a1);  }
	public static <A,B,C> void pf(String pat, A a0, B a1, C a2) {  System.out.printf(pat, a0, a1, a2);  }
	public static <A,B,C> String sf(String pat, A a0, B a1, C a2) {  return String.format(pat, a0, a1, a2);  }
	public static <A,B,C,D> void pf(String pat, A a0, B a1, C a2, D a3) {  System.out.printf(pat, a0, a1, a2, a3);  }
	public static <A,B,C,D> String sf(String pat, A a0, B a1, C a2, D a3) {  return String.format(pat, a0, a1, a2, a3);  }
	public static <A,B,C,D,E> void pf(String pat, A a0, B a1, C a2, D a3, E a4) {  System.out.printf(pat, a0, a1, a2, a3, a4);  }
	public static <A,B,C,D,E> String sf(String pat, A a0, B a1, C a2, D a3, E a4) {  return String.format(pat, a0, a1, a2, a3, a4);  }
	public static <A,B,C,D,E,F> void pf(String pat, A a0, B a1, C a2, D a3, E a4, F a5) {  System.out.printf(pat, a0, a1, a2, a3, a4, a5);  }
	public static <A,B,C,D,E,F> String sf(String pat, A a0, B a1, C a2, D a3, E a4, F a5) {  return String.format(pat, a0, a1, a2, a3, a4, a5);  }
	public static <A,B,C,D,E,F,G> void pf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6) {  System.out.printf(pat, a0, a1, a2, a3, a4, a5, a6);  }
	public static <A,B,C,D,E,F,G> String sf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6) {  return String.format(pat, a0, a1, a2, a3, a4, a5, a6);  }
	public static <A,B,C,D,E,F,G,H> void pf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7) {  System.out.printf(pat, a0, a1, a2, a3, a4, a5, a6, a7);  }
	public static <A,B,C,D,E,F,G,H> String sf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7) {  return String.format(pat, a0, a1, a2, a3, a4, a5, a6, a7);  }
	public static <A,B,C,D,E,F,G,H,I> void pf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8) {  System.out.printf(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8);  }
	public static <A,B,C,D,E,F,G,H,I> String sf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8) {  return String.format(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8);  }
	public static <A,B,C,D,E,F,G,H,I,J> void pf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9) {  System.out.printf(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9);  }
	public static <A,B,C,D,E,F,G,H,I,J> String sf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9) {  return String.format(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9);  }
	public static <A,B,C,D,E,F,G,H,I,J,K> void pf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10) {  System.out.printf(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10);  }
	public static <A,B,C,D,E,F,G,H,I,J,K> String sf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10) {  return String.format(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L> void pf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11) {  System.out.printf(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L> String sf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11) {  return String.format(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L,M> void pf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11, M a12) {  System.out.printf(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L,M> String sf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11, M a12) {  return String.format(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L,M,N> void pf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11, M a12, N a13) {  System.out.printf(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L,M,N> String sf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11, M a12, N a13) {  return String.format(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L,M,N,O> void pf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11, M a12, N a13, O a14) {  System.out.printf(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L,M,N,O> String sf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11, M a12, N a13, O a14) {  return String.format(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P> void pf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11, M a12, N a13, O a14, P a15) {  System.out.printf(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P> String sf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11, M a12, N a13, O a14, P a15) {  return String.format(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q> void pf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11, M a12, N a13, O a14, P a15, Q a16) {  System.out.printf(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q> String sf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11, M a12, N a13, O a14, P a15, Q a16) {  return String.format(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R> void pf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11, M a12, N a13, O a14, P a15, Q a16, R a17) {  System.out.printf(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R> String sf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11, M a12, N a13, O a14, P a15, Q a16, R a17) {  return String.format(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S> void pf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11, M a12, N a13, O a14, P a15, Q a16, R a17, S a18) {  System.out.printf(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S> String sf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11, M a12, N a13, O a14, P a15, Q a16, R a17, S a18) {  return String.format(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T> void pf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11, M a12, N a13, O a14, P a15, Q a16, R a17, S a18, T a19) {  System.out.printf(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19);  }
	public static <A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T> String sf(String pat, A a0, B a1, C a2, D a3, E a4, F a5, G a6, H a7, I a8, J a9, K a10, L a11, M a12, N a13, O a14, P a15, Q a16, R a17, S a18, T a19) {  return String.format(pat, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19);  }


}
