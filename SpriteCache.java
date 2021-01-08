import java.awt.image.BufferedImage;
import java.net.URL;
import javax.imageio.ImageIO;

public class SpriteCache extends ResourceCache
{
  protected Object loadResource(URL url)
  {
    try
    {
      return ImageIO.read(url);
    }
    catch (Exception e)
    {
      System.out.println("No image found at " + url);
      System.out.println("Error: "+e.getClass().getName()+" "+e.getMessage());
      //System.exit(0);
      return null;
    }
  }
      
  public BufferedImage getSprite(String name)
  {
    return (BufferedImage) getResource(name);
  }
}
