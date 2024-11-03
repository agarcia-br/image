package utils;

public class Signature {
	//@formato= filename,winicial,hinicial,wfinal,hfinal,mean_red,mean_green,mean_blue
    public String filename;
    public int winicial;
    public int hinicial;
    public int wfinal;
    public int hfinal;
    public RGB rgb;

    public int lastx=Integer.MIN_VALUE;
    public int lasty=Integer.MIN_VALUE;
    public int sqrd=Integer.MAX_VALUE;;
    
    public Signature (String s) {
    	String s2[] = s.split(",");
        filename = s2[0];
        winicial = Integer.parseInt(s2[1]);
        hinicial = Integer.parseInt(s2[2]);
        wfinal = Integer.parseInt(s2[3]);
        hfinal = Integer.parseInt(s2[4]);
        rgb = new RGB(Integer.parseInt(s2[5]),Integer.parseInt(s2[6]),Integer.parseInt(s2[7]));
    }
    
    public int getHeight() {
    	return hfinal-hinicial+1;
    }
    public int getWidth() {
    	return wfinal-winicial+1;
    }
    public boolean getVertical() {
		return getHeight()>getWidth();
	}
    public void updateRootFolder(String s) {
    	if (null==s || s.isEmpty()) return;
    	filename = s+filename.substring(s.length());
    }
    public String toString() {
    	//@formato= filename,winicial,hinicial,wfinal,hfinal,mean_red,mean_green,mean_blue
    	return filename+","+winicial+","+hinicial+","+wfinal+","+hfinal+","+rgb.r+","+rgb.g+","+rgb.b;
    }	
}
