// The RockstaR Project
// Created by Prickley of the Snippies:
// "CAP'N" Jim Culpepper, Chris "Ginger" Hoenn, and Jake "Little Foot" Newsome.
// Read the RockstaR Readme for description and controls.

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import java.applet.AudioClip; 
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.net.URL;
import java.text.*;
import java.util.*;

public class rockstar extends Canvas implements KeyListener
{
  public int xpos, ypos, lives, health = 50, boundry[][], choice, select;  
  public int ven, lev, step, picks, stuntime = 50, updowntime;
  public int swingtime = 50, distance, prevdif, prevloc, length;
  public final int appletwidth = 800, appletheight = 600;
  public final int height = appletwidth / 32, width = appletheight / 24;
  public double fps;
  public long uTime;
  public boolean up, down, left, right, jump, fire, pause, console, dead;
  public boolean facing = true, swing, stun, fin = false;
  public String bg, block, prevbg, boss, attack;
  public Font Style, Screen;
  public Graphics g;
  public ArrayList EnemyList, ItemList, MapList;
  public FileReader fil;
  public BufferStrategy strat;
  public Lava AU, AS;
  public Fire F;
  public Boss B;
  public SpriteCache sprites = new SpriteCache();
  public SoundCache sounds = new SoundCache();
  
  public static void main(String[] args)
  {
    rockstar rck = new rockstar();
    rck.GameMain();
  }

  public rockstar()
  {
    JFrame mainframe = new JFrame("RockstaR");
    mainframe.setBounds(-1, -27, appletwidth + 6, appletheight + 32);
    mainframe.setVisible(true);
    mainframe.addWindowListener(new WindowAdapter() 
    {public void windowClosing(WindowEvent e){System.exit(0);}});
    mainframe.setResizable(false);

    JPanel panel = (JPanel) mainframe.getContentPane();
    setBounds(0, 0, appletwidth, appletheight);
    panel.setPreferredSize(new Dimension(appletwidth, appletheight));
    panel.setLayout(null);
    panel.add(this);

    Style = new Font("Gargoyles Normal", Font.PLAIN, 24);
    Screen = new Font("Gargoyles Normal", Font.PLAIN, 113);

    createBufferStrategy(2);
    strat = getBufferStrategy();
    g = strat.getDrawGraphics();
    g.setFont(Style);

    requestFocus();
    addKeyListener(this);
  }

  public void GameMain()
  {
    //sounds.playSound("Opening.wav");

    g.drawImage(sprites.getSprite("opening.jpg"), 0, 0, this);
    strat.show();
    //pause(18000);

    while (true)
    {
      g.setColor(Color.black);
      g.fillRect(0,0,800,600);
      g.drawImage(sprites.getSprite("mainmenu.png"), 0, 0, this);
      g.drawImage(sprites.getSprite("iconright.gif"), 100, 198+choice*97, this);
      g.drawImage(sprites.getSprite("iconleft.gif"), 350, 198+choice*97, this);
      strat.show();
      select = mainMenu();
      
      if (select == 0)
      {
        g.drawImage(sprites.getSprite("loading.png"), 0, 0, this);
        strat.show();
        fin = false;

        lives = 3;
        ven = 8;
        lev = 3;
        picks = 0;
        newGame();

        while (!fin)
        {
          if (bg != null)
          {
            if (ven != 9)
              sounds.loopSound(bg);
            prevbg = bg;
            bg = null;
          }

          long sTime = System.currentTimeMillis();
          updateGame();
          uTime = System.currentTimeMillis() - sTime;
          fps = 1000.0 / uTime;
//          while (fps > 60.0)
//          {
//            uTime = System.currentTimeMillis() - sTime;
//            fps = 1000.0 / uTime;
//          }

          if (stuntime < 50)
            stuntime++;
          else if (stuntime == 50)
            stun = false;
        }
        sounds.stopSound(prevbg);
      }

      else if (select == 1)
        System.exit(0);
    }
  }

  public int mainMenu()
  {
    while (true)
    {
      if (up)
        choice = 0;
      else if (down)
        choice = 1;
      else if (fire)
        return choice;

      g.setColor(Color.black);
      g.fillRect(0,0,800,600);
      g.drawImage(sprites.getSprite("mainmenu.png"), 0, 0, this);
      g.drawImage(sprites.getSprite("iconright.gif"), 244, 250+choice*100, this);
      g.drawImage(sprites.getSprite("iconleft.gif"), 494, 250+choice*100, this);
      strat.show();
    }
  }

  public void updateGame() // the main loop for executing the game
  {
    while (pause)
    {
      if ((down)&&(updowntime > 3))
      {
        if (choice < 2)
          choice++;
        else
          choice = 0;
        updowntime = 0;
      }
      else if ((up)&&(updowntime > 3))
      {
        if (choice > 0)
          choice--;
        else
          choice = 2;
        updowntime = 0;
      }

      if (fire)
      {
        pause = false;
        if (choice == 1)
          newGame();
        else if (choice == 2)
        {
          choice = 0;
          fin = true;
        }
        fire = false;
      }

      if (updowntime < 5)
        updowntime++;
      paintWorld();
    }

    updateNPCs();
    updateEnv();
    if (!fin)
    {
      updatePlayer();
      if (health == 0)
      {
        paintWorld();
        g.drawImage(sprites.getSprite("cry.gif"), xpos, ypos, this);
        strat.show();
        sounds.playSound("Death.wav");
        pause(5000);

        lives--;
        if (lives > 0)
          newGame();
        else
        {
          g.drawImage(sprites.getSprite("gameover.png"), 0, 0, this);
          strat.show();
          pause(2000);

          ven = 1;
          lev = 1;
          fin = true;
        }

        health = 50;
      }
      else
        paintWorld();
    }
  }

  public void updateNPCs() // Allows all NPCs to act and updates lightning attack
  {
    int i;
    boolean xcoll, ycoll;
    Enemy E;

    if (F != null)
    {
      F.act();
      if ((F.getX() + F.getWidth() > 810)||(F.getX() + F.getWidth() < -50))
        F = null;
    }

    for(i=0;i<EnemyList.size();i++)
    {
      E = (Enemy) EnemyList.get(i);

      if ((E.getX() < -100)||(E.getY() > appletheight))
      {
        EnemyList.remove(i);
        i--;
      }
      else
      {
        if (F != null)
        {
          xcoll = false;
          ycoll = false;

          if ((F.getX() <= E.getX() + E.getWidth())&&(F.getX() >= E.getX()))
            xcoll = true; 
          else if ((F.getX() + F.getWidth() >= E.getX())&&(F.getX() + F.getWidth() <= E.getX() + E.getWidth()))
            xcoll = true;
          if ((F.getY() >= E.getY())&&(F.getY() <= E.getY() + E.getHeight()))
            ycoll = true;
          else if ((F.getY() + F.getHeight() >= E.getY())&&(F.getY() + F.getHeight() <= E.getY() + E.getHeight()))
            ycoll = true;

          if (xcoll && ycoll)
            E.setHealth(E.getHealth() - 100.0 / E.getDF());
        }

        if ((E.getHealth() == 0)&&(!E.getPicName().equals("dead")))
        {
          E.changePic("dead");
          E.setCoordinates(E.getX() - (100 - E.getWidth()) / 2, E.getY());
        }

        if ((E.getX() < 825)||(E.getPicName().equals("dead")))
          E.act();

        if (E.getHealth() == -1)
        {
          picks += E.getDF();
          EnemyList.remove(i);
          i--;
        }
      }
    }
  }

  public void updatePlayer() // allows player to act (jumping, moving, attacking, etc.)
  {
    boolean done = false, xcoll, ycoll;
    int mag = 0, var = 0, vxpos = xpos + 10, count, i, swingcount = 0;
    Enemy E;
    Item I;
    if (!facing)
      vxpos += 60;

    if (F == null)
    {
      if (jump)
      {
        step = 1;
        var = 14;
        done = false;
        while (done == false)
        {
          mag = (int) Math.pow(var,2) / 4; 
  
          if (ypos > checkup(mag, xpos, ypos))
          {
            ypos = checkup(mag, xpos, ypos);
            if (left)
              xpos = checkleft(10, xpos, ypos);
            if (right)
              xpos = checkright(10, xpos, ypos);
            stuntime = 50;
          }
          else
            done = true;
  
          if (fire)
          {
            stuntime = 50;
            if ((health < 100)&&(swingcount == 0))
            {
              swing = true;
              swingcount = 1;
              count = 0;
              step = 1;
              while (step > 0)
              {
                updateNPCs();
                updateEnv();
                paintWorld();
                count++;
                if (count == 30)
                {
                  step++;
                  if (step == 2)
                  {
                    for(i=0;i<EnemyList.size();i++)
                    {
                      xcoll = false;
                      ycoll = false;
                      E = (Enemy) EnemyList.get(i);
  
                      if ((facing)&&(E.getX() >= xpos)&&(E.getX() <= xpos + 125))
                        xcoll = true;
                      else if ((!facing)&&(E.getX() + E.getWidth() <= xpos + 125)&&(E.getX() + E.getWidth() >= xpos))
                        xcoll = true;
                      if ((E.getY() >= ypos - 50)&&(E.getY() <= ypos + 110))
                        ycoll = true;
  
                      if (xcoll && ycoll)
                      {
                        E.setHealth(E.getHealth() - 100.0 / E.getDF());
                        int r = (int) (2 * Math.random() + 1);
                        sounds.playSound("Attack" + r + ".wav");
                      }
                    }
                  }
                  count = 0;
                  pause(2);
                }
                if (step > 2)
                  step = 0;
              }
              swing = false;
            }
            fire = false;
          }
  
          for(i=0;i<ItemList.size();i++)
          {
            xcoll = false;
            ycoll = false;
            I = (Item) ItemList.get(i);
  
            if ((xpos + 100 >= I.getX())&&(xpos <= I.getX()))
              xcoll = true;
            if ((ypos + 100 >= I.getY())&&(ypos <= I.getY()))
              ycoll = true;
  
            if (xcoll && ycoll)
            {
              if ((health < 100)&&(I.getDes().equals("health")))
              {
                health += I.getMag();
                if (health > 100)
                  health = 100;
              }
              else if (I.getDes().equals("lightning"))
                realLightning();
              else if (I.getDes().equals("strings"))
                lives++;
              else if (I.getDes().equals("pick"))
                picks += I.getMag();
  
              ItemList.remove(i);
              i--;
            }
          }
  
          var--;
          updateNPCs();
          updateEnv();
          paintWorld();
          pause(1);
        }
      }
  
      else if (left)
      {
        xpos = checkleft(10, xpos, ypos);
        step = (step + 1) % 8;
      }
  
      else if (right)
      {
        xpos = checkright(10, xpos, ypos);
        step = (step + 1) % 8;
      }
  
      if (fire)
      {      
        stuntime = 50;
        if (health >= 100)
        {
          if (F == null)
          {
            F = new Fire(4, vxpos, ypos+40, 50, 50, facing);
            sounds.playSound("Fire.wav");
          }
        }
        else
        {
          swing = true;
          count = 0;
          step = 1;
          while (step > 0)
          {
            updateNPCs();
            updateEnv();
            paintWorld();
            count++;
            if (count == 30)
            {
              step++;
              if (step == 2)
              {
                for(i=0;i<EnemyList.size();i++)
                {
                  xcoll = false;
                  ycoll = false;
                  E = (Enemy) EnemyList.get(i);
  
                  if ((facing)&&(E.getX() >= xpos)&&(E.getX() <= xpos + 125))
                    xcoll = true;
                  else if ((!facing)&&(E.getX() + E.getWidth() <= xpos + 125)&&(E.getX() + E.getWidth() >= xpos))
                    xcoll = true;
                  if ((E.getY() >= ypos - 50)&&(E.getY() <= ypos + 110))
                    ycoll = true;
  
                  if (xcoll && ycoll)
                  {
                    E.setHealth(E.getHealth() - 100.0 / E.getDF());
                    int r = (int) (2 * Math.random() + 1);
                    sounds.playSound("Attack" + r + ".wav");
                  }
                }
              }
              count = 0;
              pause(2);
            }
            if (step > 2)
             step = 0;
          }
          swing = false;
        }
        fire = false;
      }

      for(i=0;i<ItemList.size();i++)
      {
        xcoll = false;
        ycoll = false;
        I = (Item) ItemList.get(i);
  
        if ((xpos + 100 >= I.getX())&&(xpos <= I.getX()))
          xcoll = true;
        if ((ypos + 100 >= I.getY())&&(ypos <= I.getY()))
          ycoll = true;
  
        if (xcoll && ycoll)
        {
          if ((health < 100)&&(I.getDes().equals("health")))
          {
            health += I.getMag();
            if (health > 100)
              health = 100;
          }
          else if (I.getDes().equals("lightning"))
            realLightning();
          else if (I.getDes().equals("strings"))
            lives++;
          else if (I.getDes().equals("pick"))
            picks += I.getMag();
  
          ItemList.remove(i);
          i--;
        }
      }
 
      done = false;
      if (var < 3)
        var = 2;
      while (done == false)
      {
        mag = (int) Math.pow(var,2) / 4; 
  
        if (ypos < checkdown(mag, xpos, ypos))
        {
          step = 1;
          ypos = checkdown(mag, xpos, ypos);
          if (left)
            xpos = checkleft(10, xpos, ypos);
          if (right)
            xpos = checkright(10, xpos, ypos);
        }
        else
          done = true;
  
        if (fire)
        {
          stuntime = 50;
          if ((health < 100)&&(swingcount == 0))
          {
            swing = true;
            swingcount = 1;
            count = 0;
            step = 1;
            while (step > 0)
            {
              updateNPCs();
              updateEnv();
              paintWorld();
              count++;
              if (count == 30)
              {
                step++;
                if (step == 2)
                {
                  for(i=0;i<EnemyList.size();i++)
                  {
                    xcoll = false;
                    ycoll = false;
                    E = (Enemy) EnemyList.get(i);
  
                    if ((facing)&&(E.getX() >= xpos)&&(E.getX() <= xpos + 125))
                      xcoll = true;
                    else if ((!facing)&&(E.getX() + E.getWidth() <= xpos + 125)&&(E.getX() + E.getWidth() >= xpos))
                      xcoll = true;
                    if ((E.getY() >= ypos - 50)&&(E.getY() <= ypos + 110))
                      ycoll = true;
  
                    if (xcoll && ycoll)
                    {
                      E.setHealth(E.getHealth() - 100.0 / E.getDF());
                      int r = (int) (2 * Math.random() + 1);
                      sounds.playSound("Attack" + r + ".wav");
                    }
                  }
                }
                count = 0;
                pause(2);
              }
              if (step > 2)
                step = 0;
            }
            swing = false;
          }
          fire = false;
        }

        for(i=0;i<ItemList.size();i++)
        {
          xcoll = false;
          ycoll = false;
          I = (Item) ItemList.get(i);
  
          if ((xpos + 100 >= I.getX())&&(xpos <= I.getX()))
            xcoll = true;
          if ((ypos + 100 >= I.getY())&&(ypos <= I.getY()))
            ycoll = true;
  
          if (xcoll && ycoll)
          {
            if ((health < 100)&&(I.getDes().equals("health")))
            {
              health += I.getMag();
              if (health > 100)
                health = 100;
            }
            else if (I.getDes().equals("lightning"))
              realLightning();
            else if (I.getDes().equals("strings"))
              lives++;
            else if (I.getDes().equals("pick"))
              picks += I.getMag();
  
            ItemList.remove(i);
            i--;
          }
        }
  
        var++;
        updateNPCs();
        updateEnv();
        paintWorld();
        pause(1);
      }
  
      if (!stun)
      {
        for(i=0;i<EnemyList.size();i++)
        {
          E = (Enemy) EnemyList.get(i);
  
          if (E.getHealth() > 0)
          {
            xcoll = false;
            ycoll = false;
            if ((xpos > E.getX())&&(xpos < E.getX() + E.getWidth()))
              xcoll = true;
            else if ((xpos + 100 > E.getX())&&(xpos < E.getX() + E.getWidth()))
              xcoll = true;
            if ((ypos > E.getY())&&(ypos < E.getY() + E.getHeight()))
              ycoll = true;
            else if ((ypos + 100 > E.getY())&&(ypos < E.getY() + E.getHeight()))
              ycoll = true;
  
            if (xcoll && ycoll)
            {
              health -= 25;
              stun = true;
              stuntime = 0;
              int r = (int) (2 * Math.random() + 1);
              sounds.playSound("Hurt" + r + ".wav");
            }
          }
        }
      }
 
      if (ypos > 475) 
        health = 0;
    }
 
  } 
 
  public void updateEnv() // moves all NPCs, items, etc. along with the environment as player moves across level
  {
    if (xpos > width * 14)
    {
      int dif = xpos - width * 14, i;
      xpos = width * 14;

      if (nextcol(1, distance + 1) != -1)
      {
        prevloc -= dif;
        prevdif += dif;

        if (prevdif >= 25)
        {
          prevdif -= 25;
          distance++;
          int j;
          for(i=1;i<39;i++)
            for(j=0;j<24;j++)
              boundry[i][j] = boundry[i+1][j];

          for(j=0;j<24;j++)
            boundry[39][j] = nextcol(j, distance); 
        }
   
        Item I;
        for(i=0;i<ItemList.size();i++)
        {
          I = (Item) ItemList.get(i);
          I.setCoordinates(I.getX() - dif, I.getY());
        }

        Enemy E;
        for(i=0;i<EnemyList.size();i++)
        {
          E = (Enemy) EnemyList.get(i);
          E.setCoordinates(E.getX() - dif, E.getY());
        }

        if (F != null)
          F.setCoordinates(F.getX() - dif, F.getY());
      }
      else
        endLevel();

    }
    else if (ven == 9)
      endLevel();
  }

  public void paintWorld()
  {
    int i, j;
    String S;
    Enemy E;
    Item I;

    g.drawImage(sprites.getSprite("" + ven + ".jpg"), prevloc, 0, this);
    for(i=1;i<32;i++)
      for(j=1;j<24;j++)
        if (boundry[i][j] != 0)
          g.drawImage(sprites.getSprite(block), i*width-prevdif, j*height, this);
    for(i=0;i<32;i++)
      g.drawImage(sprites.getSprite("magma.jpg"), i*width-prevdif, 23*height, this);
    g.drawImage(sprites.getSprite(block), appletwidth-prevdif, 22*height, this);
    g.drawImage(sprites.getSprite(block), 0-prevdif, 22*height, this);

    g.setColor(Color.yellow);
    if (console)
    {
      g.drawString("FPS: " + (int) fps, 700, 570);
      g.drawString("X: " + xpos + " Y: " + ypos, 700, 29);

      g.setColor(Color.red);
      for(i=0;i<EnemyList.size();i++)
      {
        E = (Enemy) EnemyList.get(i);
        g.drawRect(E.getX(), E.getY(), E.getWidth(), E.getHeight());
      }

      if (B != null)
      {
        g.drawRect(B.getX(), B.getY(), B.getWidth(), B.getHeight());
        if (AU != null)
          g.drawRect(AU.getX(), AU.getY(), AU.getWidth(), AU.getHeight());
        if (AS != null)
          g.drawRect(AS.getX(), AS.getY(), AS.getWidth(), AS.getHeight());
      }

      g.setColor(Color.green);
      if (F != null)
        g.drawRect(F.getX(), F.getY(), F.getWidth(), F.getHeight());

      g.setColor(Color.blue);
      g.drawRect(xpos, ypos, 110, 100);
    }

    for(i=0;i<ItemList.size();i++)
    {      
      I = (Item) ItemList.get(i);

      S = I.getDes();
      S += ".png";
      g.drawImage(sprites.getSprite(S), I.getX(), I.getY(), this);
    }

    if (AU != null)
      g.drawImage(sprites.getSprite(AU.getPicName() + ".png"), AU.getX(), AU.getY(), this);
    if (AS != null)
      g.drawImage(sprites.getSprite(AS.getPicName() + ".png"), AS.getX(), AS.getY(), this);

    for(i=0;i<EnemyList.size();i++)
    {
      E = (Enemy) EnemyList.get(i);
      S = E.getPicName();
      if (!S.equals("dead"))
      {
        if (E.attack())
          S += "p";
        if (E.getFacing())
          S += "right";
        else
          S += "left";
        S += "" + E.getStep();
      }

      S += ".gif";
      g.drawImage(sprites.getSprite(S), E.getX(), E.getY(), this);
    }

    if (B != null)
      g.drawImage(sprites.getSprite("boss" + B.getPicName() + ".png"), B.getX(), B.getY(), this);

    if ((health > 0)&&((stuntime % 2 == 0)||(swing)))
    {
      S = "warlock";
      if (facing)
        S += "right";
      else
        S += "left";
      if (swing)
        S += "swing" + step;
      else
        S += step / 4;
      S += ".gif";

      if ((swing)&&(step == 1))
      {
        if (facing)
          g.drawImage(sprites.getSprite(S), xpos-20, ypos-50, this);
        else
          g.drawImage(sprites.getSprite(S), xpos+30, ypos-50, this);
      }
      else if ((swing)&&(step == 2)&&(!facing))
        g.drawImage(sprites.getSprite(S), xpos-10, ypos, this);
      else
        g.drawImage(sprites.getSprite(S), xpos, ypos, this);
    }

    if (F != null)
      g.drawImage(sprites.getSprite("fire.png"), F.getX(), F.getY(), F.getWidth(), F.getHeight(), this);

    g.setColor(Color.white);
    g.fillRect(29, 12, 102, 20);
    g.drawImage(sprites.getSprite("icon.gif"), 140, 10, this); 
    g.drawString("x " + lives, 170, 29);
    g.drawString("VENUE:", 220, 29);
    g.drawString("" + ven + "-" + lev, 290, 29);
    g.drawString("PICKS:", 337, 29);
    g.drawString("" + picks, 398, 29);
    g.setColor(Color.red);
    g.fillRect(30, 13, health, 18);

    if (pause)
    {
      g.drawImage(sprites.getSprite("pausemenu.png"), 200, 100, this);
      g.drawImage(sprites.getSprite("iconright.gif"), 220, 248+choice*92, this);
      g.drawImage(sprites.getSprite("iconleft.gif"), 510, 248+choice*92, this);
    }

    strat.show();
  }

  public void newGame()
  {
    int i, j;
    loading();

    xpos = 100;
    ypos = 100;
    choice = 0;
    prevloc = 0;
    boundry = new int[40][24];
    step = 0;
    distance = 0;
    dead = false;
    facing = true;

    for(i=0;i<40;i++)
    {
      boundry[i][0] = 2;
      boundry[i][22] = 1;
      boundry[i][23] = 1;
    }
    for(i=0;i<24;i++)
      boundry[0][i] = 2;

    prevdif = 0;
    readmap(); 
  }

  public int checkleft(int mag, int x, int y) // checks boundries to the left of the player
  {
    int b1, b2, b3, b4, b5 = 0;

    b1 = boundry[(x-1)/width+2][y/height];
    b2 = boundry[(x-1)/width+2][y/height+1];
    b3 = boundry[(x-1)/width+1][y/height+2];
    b4 = boundry[(x-1)/width+2][y/height+3];
    if (y % 25 != 1)
      b5 = boundry[(x-1)/width+2][y/height+4];

    while ((mag > 0)&&(b1 == 0)&&(b2 == 0)&&(b3 == 0)&&(b4 == 0)&&(b5 == 0))
    {
      x--;
      mag--;
      b1 = boundry[(x-1)/width+2][y/height];
      b2 = boundry[(x-1)/width+2][y/height+1];
      b3 = boundry[(x-1)/width+1][y/height+2];
      b4 = boundry[(x-1)/width+2][y/height+3];
      if (y % 25 != 1)
        b5 = boundry[(x-1)/width+2][y/height+4];
    }
    if ((b1 != 0)||(b2 != 0)||(b3 != 0)||(b4 != 0)||(b5 != 0))
      x+=2;
    facing = false;
    return x;
  }

  public int checkright(int mag, int x, int y) // checks boundries to the right of the player
  {
    int b1, b2, b3, b4, b5 = 0;

    b1 = boundry[(x-1)/width+3][y/height];
    b2 = boundry[(x-1)/width+3][y/height+1];
    b3 = boundry[(x-1)/width+4][y/height+2];
    b4 = boundry[(x-1)/width+3][y/height+3];
    if (y % 25 != 1)
      b5 = boundry[(x-1)/width+3][y/height+4];

    while ((mag > 0)&&(b1 == 0)&&(b2 == 0)&&(b3 == 0)&&(b4 == 0)&&(b5 == 0))
    {
      x++;
      mag--;
      b1 = boundry[(x-1)/width+3][y/height];
      b2 = boundry[(x-1)/width+3][y/height+1];
      b3 = boundry[(x-1)/width+4][y/height+2];
      b4 = boundry[(x-1)/width+3][y/height+3];
      if (y % 25 != 1)
        b5 = boundry[(x-1)/width+3][y/height+4];
    }
    if ((b1 != 0)||(b2 != 0)||(b3 != 0)||(b4 != 0)||(b5 != 0))
      x-=2;

    b1 = boundry[(x-1)/width+1][y/height];
    b2 = boundry[(x-1)/width+1][y/height+1];
    b3 = boundry[(x-1)/width][y/height+2];
    b4 = boundry[(x-1)/width+1][y/height+3];

    if (y % 25 != 1)
      b5 = boundry[(x-1)/width+1][y/height+4];
    if ((b1 != 0)||(b2 != 0)||(b3 != 0)||(b4 != 0)||(b5 != 0))
      x += 14;
    if (x < 26)
      x = 26;

    facing = true;
    return x;
  }

  public int checkup(int mag, int x, int y) // checks boundries above the player
  {
    int b1 = 0, b2, b3, b4 = 0, b5 = 0;

    if (facing)
      b1 = boundry[x/width][(y-2)/height+2];
    b2 = boundry[x/width+1][(y-2)/height+1];
    b3 = boundry[x/width+2][(y-2)/height];
    if (facing)
    {
      if (x % 25 != 1)
        b4 = boundry[x/width+3][(y-2)/height+2];
    }
    else
    {
      b4 = boundry[x/width+3][(y-2)/height+2];
      if (x % 25 != 1)
        b5 = boundry[x/width+4][(y-2)/height+2];
    }


    while ((mag > 0)&&(b1 == 0)&&(b2 == 0)&&(b3 == 0)&&(b4 == 0)&&(b5 == 0))
    {
      y--;
      mag--;
      if (facing)
        b1 = boundry[x/width][(y-2)/height+2];
      b2 = boundry[x/width+1][(y-2)/height+1];
      b3 = boundry[x/width+2][(y-2)/height];
      if (facing)
      {
        if (x % 25 != 1)
          b4 = boundry[x/width+3][(y-2)/height+2];
      }
      else
      {
        b4 = boundry[x/width+3][(y-2)/height+2];
        if (x % 25 != 1)
          b5 = boundry[x/width+4][(y-2)/height+2];
      }
    }
    if ((b1 != 0)||(b2 != 0)||(b3 != 0)||(b4 != 0)||(b5 != 0))
      y++;
    return y;
  }

  public int checkdown(int mag, int x, int y) // checks boundries below the player
  {
    int b1 = 0, b2, b3, b4 = 0, b5 = 0;

    if (facing)
      b1 = boundry[x/width][(y-2)/height+4];
    b2 = boundry[x/width+1][(y-2)/height+4];
    b3 = boundry[x/width+2][(y-2)/height+4];
    if (facing)
    {
      if (x % 25 != 1)
        b4 = boundry[x/width+3][(y-2)/height+4];
    }
    else
    {
      b4 = boundry[x/width+3][(y-2)/height+4];
      if (x % 25 != 1)
        b5 = boundry[x/width+4][(y-2)/height+4];
    }

    while ((mag > 0)&&(b1 == 0)&&(b2 == 0)&&(b3 == 0)&&(b4 == 0)&&(b5 == 0))
    {
      y++;
      mag--;
      if (facing)
        b1 = boundry[x/width][(y-2)/height+4];
      b2 = boundry[x/width+1][(y-2)/height+4];
      b3 = boundry[x/width+2][(y-2)/height+4];
      if (facing)
      {
        if (x % 25 != 1)
          b4 = boundry[x/width+3][(y-2)/height+4];
      }
      else
      {
        b4 = boundry[x/width+3][(y-2)/height+4];
        if (x % 25 != 1)
          b5 = boundry[x/width+4][(y-2)/height+4];
      }
    }
    if ((b1 != 0)||(b2 != 0)||(b3 != 0)||(b4 != 0)||(b5 != 0))
      y--;
    return y;
  }

  public void readmap() // reads .dat files for BGM, blocks, enemies, and items
  {
    try
    {
      try
      {
        EnemyList = new ArrayList();
        ItemList = new ArrayList();
        MapList = new ArrayList();
        fil = new FileReader("" + ven + ".dat");

        int s, x, y, h, w, l;
        boolean b;
        String ta, p;
        StringTokenizer T;
        BufferedReader fDIS = new BufferedReader(fil);
        
        ta = fDIS.readLine();
        while (!ta.equals("Block"))
          ta = fDIS.readLine();
        block = fDIS.readLine();                
        while (!ta.equals("BG"))
          ta = fDIS.readLine();
        bg = fDIS.readLine();                
        
        fil = new FileReader("" + ven + "-" + lev + ".dat");
        fDIS = new BufferedReader(fil);
        
        ta = fDIS.readLine();
        while (!ta.equals("EnemyList"))
          ta = fDIS.readLine();
        ta = fDIS.readLine();

        while (!ta.equals("Items")) 
        {
          T = new StringTokenizer(ta,"-");
          s = Integer.parseInt(T.nextToken());
          x = Integer.parseInt(T.nextToken());
          y = Integer.parseInt(T.nextToken());
          h = Integer.parseInt(T.nextToken());
          w = Integer.parseInt(T.nextToken());
          b = Boolean.getBoolean(T.nextToken());
          l = Integer.parseInt(T.nextToken());
          p = T.nextToken();
                                                       
          try
          {
            if (p.equals("zombie"))
              EnemyList.add(new Zombie(s, x, y, h, w, b, l, p));
            else if (p.equals("gma"))
              EnemyList.add(new Gma(s, x, y, h, w, b, l, p));
            else if (p.equals("amp"))
              EnemyList.add(new Amp(s, x, y, h, w, b, l, p));
            else if (p.equals("snake"))
              EnemyList.add(new Snake(s, x, y, h, w, b, l, p));
          }

          catch (NumberFormatException e)
          {System.err.println("Invalid Character");}
          catch (NoSuchElementException e)
          {System.err.println("Error locating Enemy values");}
        
          ta = fDIS.readLine();
        }

        ta = fDIS.readLine();
        while (!ta.equals("Map"))
        {
          T = new StringTokenizer(ta,"-");
          p = T.nextToken();
          x = Integer.parseInt(T.nextToken());
          y = Integer.parseInt(T.nextToken());
          l = Integer.parseInt(T.nextToken());
                                                       
          try
          {ItemList.add(new Item(p, x, y, l));}
          catch (NumberFormatException e)
          {System.err.println("Invalid Character");}
          catch (NoSuchElementException e)
          {System.err.println("Error locating Enemy values");}

          ta = fDIS.readLine();
        }

        boss = null;
        ta = fDIS.readLine();
        if (ta.equals("BOSS"))
        {
          ta = fDIS.readLine();
          boss = ta;
        }
        else
        {
          while (ta != null)
          {
            MapList.add(ta);
            ta = fDIS.readLine();
            if ((ta != null)&&(ta.equals("BOSS")))
            {
              ta = fDIS.readLine();
              boss = ta;
              ta = fDIS.readLine();
              attack = ta;
              ta = fDIS.readLine();
            }
          }
        }

        fil.close();
      }
      
      catch (FileNotFoundException e)
      {System.err.println("Map File " + ven + " Not Found.  Unable to be loaded.");}

    }
    catch (IOException e)
    {System.err.println("Error in reading Map File " + ven + ".");}
  }

  public void realLightning()
  {}

  public int nextcol(int row, int dis) // places the next column of blocks on the map
  {
    if (row == 0)
      return 2;
    else if (MapList.size() > dis)
    {
      int count = 1;
      String line = (String) MapList.get(dis);
      StringTokenizer T = new StringTokenizer(line, "-");
      
      while (row > count)
      {
        T.nextToken();
        count++;
      }
      return Integer.parseInt(T.nextToken());

    }
    else
      return -1;
  }

  public void endLevel() // ends the level, starts boss battle (if applicable), and takes you to the next level
  {
    sounds.stopSound(prevbg);
    int i, mag, idlecount = 0;
    stuntime = 50;
    EnemyList = new ArrayList();
    AU = null;
    AS = null;

    if (boss != null)
    {
      int s, x, h, w, l, var = 0, vxpos, count, swingcount = 0;
      boolean b, done = false, xcoll, ycoll;
      String p;
      StringTokenizer T = new StringTokenizer(boss,"-");

      if (ven == 9)
        B = new Boss(5, 450, 250, 350, 300, false, 1, "final");
      else
      {
        s = Integer.parseInt(T.nextToken());
        w = Integer.parseInt(T.nextToken());
        h = Integer.parseInt(T.nextToken());
        b = Boolean.getBoolean(T.nextToken());
        l = Integer.parseInt(T.nextToken());
        p = T.nextToken();

        for(i=ypos;i<451;i++)
          ypos = i;
        if (b == true)
        {
          x = 25;
          facing = true;
          for(i=xpos;i<675;i++)
          {
            xpos = i;
            if ((xpos >= 350)&&(xpos < 475))
              ypos--;
            paintWorld();
          }
          facing = false;
        }

        else
        {
          x = 775 - w;
          facing = false;
          for(i=xpos;i>25;i--)
          {
            xpos = i;
            if ((xpos > 225)&&(xpos <= 350))
              ypos--;
            paintWorld();
          }
          facing = true;
        }
        health = 100;
        ItemList = new ArrayList();
        B = new Boss(s, x, w, h, b, l, p);

        for(i=B.getY();i<475-B.getHeight();i+=2)
        {
          paintWorld();
          if (i > 475 - B.getHeight())
            i = 475 - B.getHeight();
          B.setCoordinates(B.getX(), i);
        }
      }

      health = 100;
      paintWorld();
      g.setColor(Color.white);
      g.drawString("DIE!", 380, 340);
      strat.show();
      pause(1000);

      sounds.loopSound(prevbg);
      while ((B.getHealth() > 0)&&(health > 0))
      {
        swingcount = 0;
        idlecount++;
        paintWorld();

        while (pause)
        {
          if ((down)&&(updowntime > 3))
          {
            if (choice < 2)
              choice++;
            else
              choice = 0;
            updowntime = 0;
          }
          else if ((up)&&(updowntime > 3))
          {
            if (choice > 0)
              choice--;
            else
              choice = 2;
            updowntime = 0;
          }
  
          if (fire)
          {
            pause = false;
            if (choice == 1)
            {
              newGame();
            }
            else if (choice == 2)
            {
              choice = 0;
              health = 0;
              fin = true;
            }
            fire = false;
          }
  
          if (updowntime < 5)
            updowntime++;
          paintWorld();
        }

        B.act();

        if (idlecount / (int) B.getHealth() > 10)
          AU = new Lava((int)(100.0/B.getHealth()),xpos,575,125,300,false,attack+"up");
        if (AU != null)
        {
          AU.act();
          if (AU.getY() > appletheight)
            AU = null;
          idlecount = 0;
        }
        if (AS != null)
        {
          AS.act();
          if (-(AS.getX()) > AS.getWidth())
            AS = null;
        }

        if (jump)
        {
          step = 1;
          var = 14;
          done = false;

          while ((done == false)&&(B.getHealth() > 0)&&(health > 0))
          {
            mag = (int) Math.pow(var,2) / 4; 

            if (ypos > checkup(mag, xpos, ypos))
            {
              ypos = checkup(mag, xpos, ypos);
              if (left)
                xpos = checkleft(10, xpos, ypos);
              if (right)
                xpos = checkright(10, xpos, ypos);
              stuntime = 50;
            }
            else
              done = true;

            B.act();

            idlecount++;
            if (idlecount / (int) B.getHealth() > 50)
              AU = new Lava((int)(100.0/B.getHealth()),xpos,575,125,300,false,attack+"up");
            if (AU != null)
            {
              AU.act();
              if (AU.getY() > appletheight)
                AU = null;
              idlecount = 0;
            }
            if (AS != null)
            {
              AS.act();
              if (-(AS.getX()) > AS.getWidth())
                AS = null;
            }

            if (fire)
            {
              vxpos = xpos + 10;
              if (!facing)
                vxpos += 60;
              if (F == null)
              {
                F = new Fire(4, vxpos, ypos + 40, 50, 50, facing);
                sounds.playSound("Fire.wav");
              }
              fire = false;
            }

            if (F != null)
            {
              xcoll = false;
              ycoll = false;

              if ((F.getX() <= B.getX() + B.getWidth())&&(F.getX() >= B.getX()))
                xcoll = true; 
              else if ((F.getX() + F.getWidth() >= B.getX())&&(F.getX() + F.getWidth() <= B.getX() + B.getWidth()))
                xcoll = true;
              if ((F.getY() >= B.getY())&&(F.getY() <= B.getY() + B.getHeight()))
                ycoll = true;
              else if ((F.getY() + F.getHeight() >= B.getY())&&(F.getY() + F.getHeight() <= B.getY() + B.getHeight()))
                ycoll = true;

              if (xcoll && ycoll)
              {
                B.setHealth(B.getHealth() - 100.0 / B.getDF());
                F = null;
                AU = new Lava((int)(100.0/B.getHealth()),xpos,575,125,300,false,attack+"up");
                AS = new Lava((int)(100.0/B.getHealth()),-275,ypos,300,125,true,attack+"side");
              }
            }

            if (B.getHealth() > 0)
            {
              xcoll = false;
              ycoll = false;
     
              if ((xpos > B.getX())&&(xpos < B.getX() + B.getWidth()))
                xcoll = true;
              else if ((xpos + 100 > B.getX())&&(xpos < B.getX() + B.getWidth()))
                xcoll = true;
              if ((ypos > B.getY())&&(ypos < B.getY() + B.getHeight()))
                ycoll = true;
              else if ((ypos + 100 > B.getY())&&(ypos < B.getY() + B.getHeight()))
                ycoll = true;
               
              if (xcoll && ycoll)
                health = 0;
    
              xcoll = false;
              ycoll = false;
         
              if (AU != null)
              {
                if ((xpos > AU.getX())&&(xpos < AU.getX() + AU.getWidth()))
                  xcoll = true;
                else if ((xpos + 100 > AU.getX())&&(xpos < AU.getX() + AU.getWidth()))
                  xcoll = true;
                if ((ypos > AU.getY())&&(ypos < AU.getY() + AU.getHeight()))
                  ycoll = true;
                else if ((ypos + 100 > AU.getY())&&(ypos < AU.getY() + AU.getHeight()))
                  ycoll = true;
              }  
     
              if (xcoll && ycoll)
                health = 0;
          
              xcoll = false;
              ycoll = false;

              if (AS != null)
              {
                if ((xpos > AS.getX())&&(xpos < AS.getX() + AS.getWidth()))
                  xcoll = true;
                else if ((xpos + 100 > AS.getX())&&(xpos < AS.getX() + AS.getWidth()))
                  xcoll = true;
                if ((ypos > AS.getY())&&(ypos < AS.getY() + AS.getHeight()))
                  ycoll = true;
                else if ((ypos + 100 > AS.getY())&&(ypos < AS.getY() + AS.getHeight()))
                  ycoll = true;
              }
               
              if (xcoll && ycoll)
                health = 0;
            }

            var--;
            updateNPCs();
            paintWorld();
            pause(1);
          }
        }

        else if (left)
        {
          xpos = checkleft(10, xpos, ypos);
          step = (step + 1) % 8;
        }

        else if (right)
        {
          xpos = checkright(10, xpos, ypos);
          step = (step + 1) % 8;
        }

        if (fire)
        {      
          vxpos = xpos + 10;
          if (!facing)
            vxpos += 60;
          if (F == null)
          {
            F = new Fire(4, vxpos, ypos + 40, 50, 50, facing);
            sounds.playSound("Fire.wav");
          }
          fire = false;
        }

        done = false;
        if (var < 3)
          var = 2;
        while ((done == false)&&(B.getHealth() > 0)&&(health > 0))
        {
          mag = (int) Math.pow(var,2) / 4; 

          if (ypos < checkdown(mag, xpos, ypos))
          {
            step = 1;
            ypos = checkdown(mag, xpos, ypos);
            if (left)
              xpos = checkleft(10, xpos, ypos);
            if (right)
              xpos = checkright(10, xpos, ypos);
          }
          else
            done = true;

          B.act();

          idlecount++;
          if (idlecount / (int) B.getHealth() > 50)
            AU = new Lava((int)(100.0/B.getHealth()),xpos,575,125,300,false,attack+"up");
          if (AU != null)
          {
            AU.act();
            if (AU.getY() > appletheight)
              AU = null;
            idlecount = 0;
          }
          if (AS != null)
          {
            AS.act();
            if (-(AS.getX()) > AS.getWidth())
              AS = null;
          }

          if (fire)
          {
            vxpos = xpos + 10;
            if (!facing)
              vxpos += 60;
            if (F == null)
            {
              F = new Fire(4, vxpos, ypos + 40, 50, 50, facing);
              sounds.playSound("Fire.wav");
            }
            fire = false;
          }

          if (F != null)
          {
            xcoll = false;
            ycoll = false;

            if ((F.getX() <= B.getX() + B.getWidth())&&(F.getX() >= B.getX()))
              xcoll = true; 
            else if ((F.getX() + F.getWidth() >= B.getX())&&(F.getX() + F.getWidth() <= B.getX() + B.getWidth()))
              xcoll = true;
            if ((F.getY() >= B.getY())&&(F.getY() <= B.getY() + B.getHeight()))
              ycoll = true;
            else if ((F.getY() + F.getHeight() >= B.getY())&&(F.getY() + F.getHeight() <= B.getY() + B.getHeight()))
              ycoll = true;

            if (xcoll && ycoll)
            {
              B.setHealth(B.getHealth() - 100.0 / B.getDF());
              F = null;
              AU = new Lava((int)(100.0/B.getHealth()),xpos,575,125,300,false,attack+"up");
              AS = new Lava((int)(100.0/B.getHealth()),-275,ypos,300,125,true,attack+"side");
            }
          }

          if (B.getHealth() > 0)
          {
            xcoll = false;
            ycoll = false;
     
            if ((xpos > B.getX())&&(xpos < B.getX() + B.getWidth()))
              xcoll = true;
            else if ((xpos + 100 > B.getX())&&(xpos < B.getX() + B.getWidth()))
              xcoll = true;
            if ((ypos > B.getY())&&(ypos < B.getY() + B.getHeight()))
              ycoll = true;
            else if ((ypos + 100 > B.getY())&&(ypos < B.getY() + B.getHeight()))
              ycoll = true;
             
            if (!xcoll || !ycoll)
            {
              xcoll = false;
              ycoll = false;
       
              if (AU != null)
              {
                if ((xpos > AU.getX())&&(xpos < AU.getX() + AU.getWidth()))
                  xcoll = true;
                else if ((xpos + 100 > AU.getX())&&(xpos < AU.getX() + AU.getWidth()))
                  xcoll = true;
                if ((ypos > AU.getY())&&(ypos < AU.getY() + AU.getHeight()))
                  ycoll = true;
                else if ((ypos + 100 > AU.getY())&&(ypos < AU.getY() + AU.getHeight()))
                  ycoll = true;
              }
            }

            if (!xcoll || !ycoll)
            {
              xcoll = false;
              ycoll = false;
         
              if (AS != null)
              {
                if ((xpos > AS.getX())&&(xpos < AS.getX() + AS.getWidth()))
                  xcoll = true;
                else if ((xpos + 100 > AS.getX())&&(xpos < AS.getX() + AS.getWidth()))
                  xcoll = true;
                if ((ypos > AS.getY())&&(ypos < AS.getY() + AS.getHeight()))
                  ycoll = true;
                else if ((ypos + 100 > AS.getY())&&(ypos < AS.getY() + AS.getHeight()))
                  ycoll = true;
              }
            }

            if (xcoll && ycoll)
              health = 0;
          }

          var++;
          updateNPCs();
          paintWorld();
          pause(1);
        }

        if (F != null)
        {
          xcoll = false;
          ycoll = false;

          if ((F.getX() <= B.getX() + B.getWidth())&&(F.getX() >= B.getX()))
            xcoll = true; 
          else if ((F.getX() + F.getWidth() >= B.getX())&&(F.getX() + F.getWidth() <= B.getX() + B.getWidth()))
            xcoll = true;
          if ((F.getY() >= B.getY())&&(F.getY() <= B.getY() + B.getHeight()))
            ycoll = true;
          else if ((F.getY() + F.getHeight() >= B.getY())&&(F.getY() + F.getHeight() <= B.getY() + B.getHeight()))
            ycoll = true;

          if (xcoll && ycoll)
          {
            B.setHealth(B.getHealth() - 100.0 / B.getDF());
            F = null;
            AU = new Lava((int)(100.0/B.getHealth()),xpos,575,125,300,false,attack+"up");
            AS = new Lava((int)(100.0/B.getHealth()),-275,ypos,300,125,true,attack+"side");
          }
        }

        if (B.getHealth() > 0)
        {
          xcoll = false;
          ycoll = false;

          if ((xpos > B.getX())&&(xpos < B.getX() + B.getWidth()))
            xcoll = true;
          else if ((xpos + 100 > B.getX())&&(xpos < B.getX() + B.getWidth()))
            xcoll = true;
          if ((ypos > B.getY())&&(ypos < B.getY() + B.getHeight()))
            ycoll = true;
          else if ((ypos + 100 > B.getY())&&(ypos < B.getY() + B.getHeight()))
            ycoll = true;

          if (!xcoll || !ycoll)
          {
            xcoll = false;
            ycoll = false;
       
            if (AU != null)
            {
              if ((xpos > AU.getX())&&(xpos < AU.getX() + AU.getWidth()))
                xcoll = true;
              else if ((xpos + 100 > AU.getX())&&(xpos < AU.getX() + AU.getWidth()))
                xcoll = true;
              if ((ypos > AU.getY())&&(ypos < AU.getY() + AU.getHeight()))
                ycoll = true;
              else if ((ypos + 100 > AU.getY())&&(ypos < AU.getY() + AU.getHeight()))
                ycoll = true;
            }
          }

          if (!xcoll || !ycoll)
          {
            xcoll = false;
            ycoll = false;
        
            if (AS != null)
            {
              if ((xpos > AS.getX())&&(xpos < AS.getX() + AS.getWidth()))
                xcoll = true;
              else if ((xpos + 100 > AS.getX())&&(xpos < AS.getX() + AS.getWidth()))
                xcoll = true;
              if ((ypos > AS.getY())&&(ypos < AS.getY() + AS.getHeight()))
                ycoll = true;
              else if ((ypos + 100 > AS.getY())&&(ypos < AS.getY() + AS.getHeight()))
                ycoll = true;
            }
          }

          if (xcoll && ycoll)
            health = 0;

        }

        updateNPCs();
        paintWorld();
      }

      F = null;
      p = B.getPicName();
      h = B.getX();
      w = B.getY();
      s = B.getDF();
      B = null;
      AU = null;
      AS = null;

      if (health > 0)
      {
        sounds.stopSound(prevbg);

        for(i=1;i<3;i++)
        {
          paintWorld();
          g.drawImage(sprites.getSprite("boss" + p + "dead" + i + ".png"), h, w, this);
          strat.show();
          pause(2000);
        }

        if (p.equals("final"))
        {
          for(i=3;i<7;i++)
          {
            paintWorld();
            g.drawImage(sprites.getSprite("boss" + p + "dead" + i + ".png"), h, w, this);
            strat.show();
            pause(2000);
          }
          paintWorld();
        }

        picks += 22 * s;
      }
    }

    if (health > 0)
    {
      if (ven != 9)
      {
        sounds.playSound("Victory.wav");
        pause(4500);
      }

      lev++;
      if (lev == 4)
      {
        ven++;
        lev = 1;
      }

      if (ven == 9)
      {
        if (lev == 1)
        {
          step = 1;
          ven = 8;

          for(i=0;i<40;i++)
          {
            boundry[i][17] = 0;
            boundry[i][22] = 0;
            boundry[i][23] = 0;
          }
          for(i=0;ypos<800;i++)
          {
            mag = (int) Math.pow(i,2) / 4; 
            ypos += mag;
            paintWorld();
            pause(10);
          }
          ven = 9;
          newGame();
          health = 100;
          for(i=0;i<40;i++)
          {
            boundry[i][22] = 1;
            boundry[i][23] = 1;
          }
          xpos = 100;
          ypos = -200;
          paintWorld();
          for(i=0;ypos<451;i++)
          {
            mag = (int) Math.pow(i,2) / 4; 
            ypos += mag;
            if (ypos > 451)
              ypos = 451;
            paintWorld();
            pause(1);
          }
        }
        else if (lev == 2)
        {
          String S = "" + picks;

          sounds.playSound("Ultimate.wav");
          pause(2000);
          g.setFont(Screen);
          g.setColor(Color.white);
          g.drawImage(sprites.getSprite("final.png"), 0, 0, this);
          g.drawString(S, 400 - S.length() * 35 / 2, 560);
          strat.show();
          pause(24000);
          g.setFont(Style);

          // credits();
          fin = true;
        }
      }       
      else
      {
        g.drawImage(sprites.getSprite("loading.png"), 0, 0, this);
        strat.show();
        newGame();
      }
    }
    else
    {
      paintWorld();
      g.drawImage(sprites.getSprite("cry.gif"), xpos, ypos, this);
      strat.show();
      sounds.playSound("Death.wav");
      pause(5000);

      lives--;
      if (lives > 0)
        newGame();
      else
      {
        g.drawImage(sprites.getSprite("gameover.png"), 0, 0, this);
        strat.show();
        pause(2000);

        ven = 8;
        lev = 3;
        fin = true;
      }

      health = 50;
    }
  }

  public void loading()
  {
    g.setColor(Color.white);
    g.drawImage(sprites.getSprite("" + ven + ".jpg"), prevloc, 0, this);
    g.drawImage(sprites.getSprite("loading.png"), prevloc, 0, this);
  }

  public void credits()
  {
    int i;
    for(i=0;i>-5000;i--)
    {
      g.drawImage(sprites.getSprite("credits.jpg"), i, 0, this);
      strat.show();
      pause(1);
    }
  }

  public void pause(int spd)
  {
    try{Thread.sleep(spd);}
    catch(InterruptedException e){}
  }
       
  public void keyTyped(KeyEvent e){}
  public void keyPressed(KeyEvent e)
  {
    switch (e.getKeyCode())
    {
      case KeyEvent.VK_LEFT: left = true; break;
      case KeyEvent.VK_RIGHT: right = true; break;
      case KeyEvent.VK_SPACE: jump = true; break;
      case KeyEvent.VK_DOWN: down = true; break;
      case KeyEvent.VK_UP: up = true; break;
      case KeyEvent.VK_ENTER:
        if (pause == true)
          pause = false;
        else
          pause = true;
        break;
      case KeyEvent.VK_F1: 
        if (console == true)
          console = false;
        else
          console = true;
        break;
      case KeyEvent.VK_Z: fire = true; break;
    }
  }

  public void keyReleased(KeyEvent e)
  {
    switch (e.getKeyCode())
    {
      case KeyEvent.VK_LEFT: left = false; break;
      case KeyEvent.VK_RIGHT: right = false; break;
      case KeyEvent.VK_SPACE: jump = false; break;
      case KeyEvent.VK_DOWN: down = false; break;
      case KeyEvent.VK_UP: up = false; break;
      case KeyEvent.VK_Z: fire = false; break;
    }
  }

  abstract class NPC
  {
    protected int speed, x, y, w, h;
    protected boolean facing;

    NPC()
    {
      speed = 1;
      x = 400;
      y = 400;
      h = 0;
      w = 0;
      step = 0;
      facing = true;
    }

    NPC(int s, int X, int Y, int W, int H, boolean r)
    {
      speed = s;
      x = X;
      y = Y;
      w = W;
      h = H;
      facing = r;
    }

    public void setSpeed(int s)
    {
      speed = s;
    }

    public void setCoordinates(int newx, int newy)
    {
      x = newx;
      y = newy;
    }

    public void setDimensions(int H, int W)
    {
      h = H;
      w = W;
    }

    public void setFacing(boolean right)
    {
      facing = right;
    }

    public abstract void act();

    public int getSpeed()
    {
      return speed;
    }  

    public int getX()
    {
      return x;
    }

    public int getY()
    {
      return y;
    }

    public int getHeight()
    {
      return h;
    }

    public int getWidth()
    {
      return w;
    }

    public boolean getFacing()
    {
      return facing;
    }
  }

  class Enemy extends NPC
  {
    protected int damagefactor = 1, step, prevvar, stepcount;
    protected double health;
    protected String PicName;

    Enemy(int s, int X, int Y, int W, int H, boolean r, double hlth, int df, String P)
    {
      super(s, X, Y, W, H, r);
      health = hlth;
      damagefactor = df;
      prevvar = 0;
      PicName = P;
    }

    public void act()
    {
      if (health <= 0)
      {
        prevvar++;
        if (prevvar >= 20)
          health = -1;
      }
                     
      else if (x <= 25)
      {
        facing = false;
        x -= speed;
      }

      else if (y >= appletheight - h - 25)
      {
        if (prevvar > 0)
          prevvar = 0;
        prevvar--;
        y += (int) Math.pow(prevvar, 2) / 2;
      }

      else
      {
        if (facing)
        {
          if (x == checkright(speed, x, y))
            facing = false;
          else
            x = checkright(speed, x, y);
        }
        else
        {
          if (x == checkleft(speed, x, y))
            facing = true;
          else
            x = checkleft(speed, x, y);
        }

        prevvar--;
        int mag = (int) Math.pow(prevvar, 2) / 2;

        if (prevvar > 1)
        {
          if (y == checkup(mag, x, y))
            prevvar = 0;
          else
            y = checkup(mag, x, y);
        }
        else if (prevvar < -1)
        {
          if (y == checkdown(mag, x, y))
            prevvar = 0;
          else
            y = checkdown(mag, x, y);
        }
      }

      stepcount++;
      if (stepcount == 30)
      {
        incrementStep();
        stepcount = 0;
      }
    }

    public boolean attack()
    {
      return false;
    }

    public void setHealth(double h)
    {
      health = h;
      if (health < 0)
       health = 0;
    }

    public void changeDF(int df)
    {
      damagefactor = df;
    }

    public void changePic(String newPicName)
    {
      PicName = newPicName;
    }

    public void setStep(int s)
    {
      step = s;
    }

    public double getHealth()
    {
      return health;
    }

    public int getDF()
    {
      return damagefactor;
    }

    public int getStep()
    {
      return step;
    }

    public String getPicName()
    {
      return PicName;
    }

    public int checkleft(int mag, int x, int y)
    {
      int b1, b2, b3, b4;

      b1 = boundry[(x-1)/width][y/height];
      b2 = boundry[(x-1)/width][y/height+1];
      b3 = boundry[(x-1)/width][y/height+2];
      b4 = boundry[(x-1)/width][y/height+3];

      while ((mag > 0)&&(b1 != 1)&&(b2 != 1)&&(b3 != 1)&&(b4 != 1))
      {
        x--;
        mag--;
        b1 = boundry[(x-1)/width][y/height];
        b2 = boundry[(x-1)/width][y/height+1];
        b3 = boundry[(x-1)/width][y/height+2];
        b4 = boundry[(x-1)/width][y/height+3];
      }
      return x;
    }

    public int checkright(int mag, int x, int y)
    {
      int b1, b2, b3, b4;

      b1 = boundry[(x-1)/width+3][y/height];
      b2 = boundry[(x-1)/width+3][y/height+1];
      b3 = boundry[(x-1)/width+3][y/height+2];
      b4 = boundry[(x-1)/width+3][y/height+3];

      while ((mag > 0)&&(b1 != 1)&&(b2 != 1)&&(b3 != 1)&&(b4 != 1))
      {
        x++;
        mag--;
        b1 = boundry[(x-1)/width+3][y/height];
        b2 = boundry[(x-1)/width+3][y/height+1];
        b3 = boundry[(x-1)/width+3][y/height+2];
        b4 = boundry[(x-1)/width+3][y/height+3];
      }
      return x;
    }

    public int checkup(int mag, int x, int y)
    {
      int b1, b2, b3, b4;

      b1 = boundry[x/width][(y-2)/height];
      b2 = boundry[x/width+1][(y-2)/height];
      b3 = boundry[x/width+2][(y-2)/height];
      b4 = boundry[x/width+3][(y-2)/height];
    
      while ((mag > 0)&&(b1 != 1)&&(b2 != 1)&&(b3 != 1)&&(b4 != 1))
      {
        y--;
        mag--;
        b1 = boundry[x/width][(y-2)/height];
        b2 = boundry[x/width+1][(y-2)/height];
        b3 = boundry[x/width+2][(y-2)/height];
        b4 = boundry[x/width+3][(y-2)/height];
      }
      if ((b1 == 1)||(b2 == 1)||(b3 == 1)||(b4 == 1))
        y++;
      return y;
    }

    public int checkdown(int mag, int x, int y)
    {
      int b1, b2, b3, b4;

      b1 = boundry[x/width][(y-2)/height+4];
      b2 = boundry[x/width+1][(y-2)/height+4];
      b3 = boundry[x/width+2][(y-2)/height+4];
      b4 = boundry[x/width+3][(y-2)/height+4];

      while ((mag > 0)&&(b1 != 1)&&(b2 != 1)&&(b3 != 1)&&(b4 != 1))
      {
        y++;
        mag--;
        b1 = boundry[x/width][(y-2)/height+4];
        b2 = boundry[x/width+1][(y-2)/height+4];
        b3 = boundry[x/width+2][(y-2)/height+4];
        b4 = boundry[x/width+3][(y-2)/height+4];
      }
      if ((b1 == 1)||(b2 == 1)||(b3 == 1)||(b4 == 1))
        y--;
      return y;
    }

    public void incrementStep()
    {
      step = 0;
    }
  }

  class Fire extends NPC
  {
    Fire(int s, int X, int Y, int W, int H, boolean r)
    {
      super(s, X, Y, W, H, r);
    }

    public void act()
    {
      if (facing)
        w += 25;
      else
        w -= 25;
    }

  }

  class Zombie extends Enemy
  {
    Zombie(int s, int X, int Y, int W, int H, boolean r, int hlth, String P)
    {
      super(s, X, Y, W, H, r, hlth, 1, P);
    }

    public int checkleft(int mag, int x, int y)
    {
      int b1, b2, b3, b4;

      b1 = boundry[(x-1)/width][y/height];
      b2 = boundry[(x-1)/width][y/height+1];
      b3 = boundry[(x-1)/width][y/height+2];
      b4 = boundry[(x-1)/width][y/height+3];

      while ((mag > 0)&&(b1 == 0)&&(b2 == 0)&&(b3 == 0)&&(b4 == 0))
      {
        x--;
        mag--;
        b1 = boundry[(x-1)/width][y/height];
        b2 = boundry[(x-1)/width][y/height+1];
        b3 = boundry[(x-1)/width][y/height+2];
        b4 = boundry[(x-1)/width][y/height+3];
      }
      return x;
    }

    public int checkright(int mag, int x, int y)
    {
      int b1, b2, b3, b4;

      b1 = boundry[(x-1)/width+2][y/height];
      b2 = boundry[(x-1)/width+2][y/height+1];
      b3 = boundry[(x-1)/width+2][y/height+2];
      b4 = boundry[(x-1)/width+2][y/height+3];

      while ((mag > 0)&&(b1 == 0)&&(b2 == 0)&&(b3 == 0)&&(b4 == 0))
      {
        x++;
        mag--;
        b1 = boundry[(x-1)/width+2][y/height];
        b2 = boundry[(x-1)/width+2][y/height+1];
        b3 = boundry[(x-1)/width+2][y/height+2];
        b4 = boundry[(x-1)/width+2][y/height+3];
      }
      return x;
    }

    public int checkdown(int mag, int x, int y)
    {
      int b1, b2, b3 = 0;

      b1 = boundry[x/width][(y-2)/height+4];
      b2 = boundry[x/width+1][(y-2)/height+4];
      if (x % 25 != 0)
        b3 = boundry[x/width+2][(y-2)/height+4];

      while ((mag > 0)&&(b1 == 0)&&(b2 == 0)&&(b3 == 0))
      {
        y++;
        mag--;
        b1 = boundry[x/width][(y-2)/height+4];
        b2 = boundry[x/width+1][(y-2)/height+4];
        if (x % 25 != 0)
          b3 = boundry[x/width+2][(y-2)/height+4];
      }
      if ((b1 != 0)||(b2 != 0)||(b3 != 0))
        y--;
      return y;
    }

    public void incrementStep()
    {
      step = 0;
    }
  }

  class Gma extends Enemy
  {
    Gma(int s, int X, int Y, int W, int H, boolean r, int hlth, String P)
    {
      super(s, X, Y, W, H, r, hlth, 1, P);
    }

    public boolean attack()
    {
      if ((ypos > y - 50)&&(ypos < y + 50))
      {
        if ((!facing)&&(xpos > x - 100)&&(xpos < x + 50))
          return true;
        if ((facing)&&(xpos < x + 100)&&(xpos > x - 50))
          return true;
      }
      return false;
    }

    public int checkleft(int mag, int x, int y)
    {
      int b1, b2, b3, b4;

      b1 = boundry[(x-1)/width][y/height];
      b2 = boundry[(x-1)/width][y/height+1];
      b3 = boundry[(x-1)/width][y/height+2];
      b4 = boundry[(x-1)/width][y/height+3];

      while ((mag > 0)&&(b1 == 0)&&(b2 == 0)&&(b3 == 0)&&(b4 == 0))
      {
        x--;
        mag--;
        b1 = boundry[(x-1)/width][y/height];
        b2 = boundry[(x-1)/width][y/height+1];
        b3 = boundry[(x-1)/width][y/height+2];
        b4 = boundry[(x-1)/width][y/height+3];
      }
      return x;
    }

    public int checkright(int mag, int x, int y)
    {
      int b1, b2, b3, b4;

      b1 = boundry[(x-1)/width+2][y/height];
      b2 = boundry[(x-1)/width+2][y/height+1];
      b3 = boundry[(x-1)/width+2][y/height+2];
      b4 = boundry[(x-1)/width+2][y/height+3];

      while ((mag > 0)&&(b1 == 0)&&(b2 == 0)&&(b3 == 0)&&(b4 == 0))
      {
        x++;
        mag--;
        b1 = boundry[(x-1)/width+2][y/height];
        b2 = boundry[(x-1)/width+2][y/height+1];
        b3 = boundry[(x-1)/width+2][y/height+2];
        b4 = boundry[(x-1)/width+2][y/height+3];
      }
      return x;
    }

    public int checkdown(int mag, int x, int y)
    {
      int b1, b2, b3 = 0;

      b1 = boundry[x/width][(y-2)/height+4];
      b2 = boundry[x/width+1][(y-2)/height+4];
      if (x % 25 != 0)
        b3 = boundry[x/width+2][(y-2)/height+4];

      while ((mag > 0)&&(b1 == 0)&&(b2 == 0)&&(b3 == 0))
      {
        y++;
        mag--;
        b1 = boundry[x/width][(y-2)/height+4];
        b2 = boundry[x/width+1][(y-2)/height+4];
        if (x % 25 != 0)
          b3 = boundry[x/width+2][(y-2)/height+4];
      }
      if ((b1 != 0)||(b2 != 0)||(b3 != 0))
        y--;
      return y;
    }

  }

  class Amp extends Enemy
  {
    Amp(int s, int X, int Y, int W, int H, boolean r, int hlth, String P)
    {
      super(s, X, Y, W, H, r, hlth, 1, P);
    }

    public int checkleft(int mag, int x, int y)
    {
      int b1, b2, b3;

      b1 = boundry[(x-1)/width][y/height];
      b2 = boundry[(x-1)/width][y/height+1];
      b3 = boundry[(x-1)/width][y/height+2];

      while ((mag > 0)&&(b1 == 0)&&(b2 == 0)&&(b3 == 0))
      {
        x--;
        mag--;
        b1 = boundry[(x-1)/width][y/height];
        b2 = boundry[(x-1)/width][y/height+1];
        b3 = boundry[(x-1)/width][y/height+2];
      }
      return x;
    }

    public int checkright(int mag, int x, int y)
    {
      int b1, b2, b3;

      b1 = boundry[(x-1)/width+2][y/height];
      b2 = boundry[(x-1)/width+2][y/height+1];
      b3 = boundry[(x-1)/width+2][y/height+2];

      while ((mag > 0)&&(b1 == 0)&&(b2 == 0)&&(b3 == 0))
      {
        x++;
        mag--;
        b1 = boundry[(x-1)/width+2][y/height];
        b2 = boundry[(x-1)/width+2][y/height+1];
        b3 = boundry[(x-1)/width+2][y/height+2];
      }
      return x;
    }

    public int checkdown(int mag, int x, int y)
    {
      int b1, b2, b3, b4 = 0;

      b1 = boundry[x/width][(y-2)/height+3];
      b2 = boundry[x/width+1][(y-2)/height+3];
      b3 = boundry[x/width+2][(y-2)/height+3];
      if (x % 25 != 0)
        b4 = boundry[x/width+3][(y-2)/height+3];

      while ((mag > 0)&&(b1 == 0)&&(b2 == 0)&&(b3 == 0)&&(b4 == 0))
      {
        y++;
        mag--;
        b1 = boundry[x/width][(y-2)/height+3];
        b2 = boundry[x/width+1][(y-2)/height+3];
        b3 = boundry[x/width+2][(y-2)/height+3];
        if (x % 25 != 0)
          b4 = boundry[x/width+3][(y-2)/height+3];
      }
      if ((b1 != 0)||(b2 != 0)||(b3 != 0)||(b4 != 0))
        y--;
      return y;
    }

    public void incrementStep()
    {
      step = (step + 1) % 2;
    }
  }

  class Snake extends Enemy
  {
    Snake(int s, int X, int Y, int W, int H, boolean r, int hlth, String P)
    {
      super(s, X, Y, W, H, r, hlth, 1, P);
    }

    public int checkleft(int mag, int x, int y)
    {
      int b1, b2, b3, b4, b5 = 0;

      b1 = boundry[(x-1)/width][y/height];
      b2 = boundry[(x-1)/width][y/height+1];
      b3 = boundry[(x-1)/width][y/height+2];
      b4 = boundry[(x-1)/width][y/height+3];
      if (y % 25 != 1)
        b5 = boundry[(x-1)/width][y/height+4];

      while ((mag > 0)&&(b1 == 0)&&(b2 == 0)&&(b3 == 0)&&(b4 == 0)&&(b5 == 0))
      {
        x--;
        mag--;
        b1 = boundry[(x-1)/width][y/height];
        b2 = boundry[(x-1)/width][y/height+1];
        b3 = boundry[(x-1)/width][y/height+2];
        b4 = boundry[(x-1)/width][y/height+3];
        if (y % 25 != 1)
          b5 = boundry[(x-1)/width][y/height+4];
      }
      return x;
    }

    public int checkright(int mag, int x, int y)
    {
      int b1, b2, b3, b4, b5 = 0;

      b1 = boundry[(x-1)/width+4][y/height];
      b2 = boundry[(x-1)/width+4][y/height+1];
      b3 = boundry[(x-1)/width+4][y/height+2];
      b4 = boundry[(x-1)/width+4][y/height+3];
      if (y % 25 != 1)
        b5 = boundry[(x-1)/width+4][y/height+4];

      while ((mag > 0)&&(b1 == 0)&&(b2 == 0)&&(b3 == 0)&&(b4 == 0)&&(b5 == 0))
      {
        x++;
        mag--;
        b1 = boundry[(x-1)/width+4][y/height];
        b2 = boundry[(x-1)/width+4][y/height+1];
        b3 = boundry[(x-1)/width+4][y/height+2];
        b4 = boundry[(x-1)/width+4][y/height+3];
        if (y % 25 != 1)
          b5 = boundry[(x-1)/width+4][y/height+4];
      }
      return x;
    }

    public int checkup(int mag, int x, int y)
    {
      int b1, b2, b3, b4, b5 = 0;

      b1 = boundry[x/width][(y-2)/height];
      b2 = boundry[x/width+1][(y-2)/height];
      b3 = boundry[x/width+2][(y-2)/height];
      b4 = boundry[x/width+3][(y-2)/height];
      if (x % 25 != 0)
        b5 = boundry[x/width+4][(y-2)/height];

      while ((mag > 0)&&(b1 == 0)&&(b2 == 0)&&(b3 == 0)&&(b4 == 0)&&(b5 == 0))
      {
        y--;
        mag--;
        b1 = boundry[x/width][(y-2)/height];
        b2 = boundry[x/width+1][(y-2)/height];
        b3 = boundry[x/width+2][(y-2)/height];
        b4 = boundry[x/width+3][(y-2)/height];
        if (x % 25 != 0)
          b5 = boundry[x/width+4][(y-2)/height];
      }
      if ((b1 != 0)||(b2 != 0)||(b3 != 0)||(b4 != 0)||(b5 != 0))
        y++;
      return y;
    }

    public int checkdown(int mag, int x, int y)
    {
      int b1, b2, b3, b4, b5 = 0;

      b1 = boundry[x/width][(y-2)/height+4];
      b2 = boundry[x/width+1][(y-2)/height+4];
      b3 = boundry[x/width+2][(y-2)/height+4];
      b4 = boundry[x/width+3][(y-2)/height+4];
      if (x % 25 != 0)
        b5 = boundry[x/width+4][(y-2)/height+4];

      while ((mag > 0)&&(b1 == 0)&&(b2 == 0)&&(b3 == 0)&&(b4 == 0)&&(b5 == 0))
      {
        y++;
        mag--;
        b1 = boundry[x/width][(y-2)/height+4];
        b2 = boundry[x/width+1][(y-2)/height+4];
        b3 = boundry[x/width+2][(y-2)/height+4];
        b4 = boundry[x/width+3][(y-2)/height+4];
        if (x % 25 != 0)
          b5 = boundry[x/width+4][(y-2)/height+4];
      }
      if ((b1 != 0)||(b2 != 0)||(b3 != 0)||(b4 != 0)||(b5 != 0))
        y--;
      return y;
    }

    public void incrementStep()
    {
      step = (step + 1) % 2;
    }
  }

  class Boss extends Enemy
  {
    Boss(int s, int X, int W, int H, boolean r, int d, String P)
    {
      super(s, X, -H, W, H, r, 100, d, P);
    }

    Boss(int s, int X, int Y, int W, int H, boolean r, int d, String P)
    {
      super(s, X, Y, W, H, r, 100, d, P);
    }

    public void act()
    {
      if (xpos >= x)
        facing = true;
      else
        facing = false;
    }
  }

  class Lava extends Enemy
  {
    Lava(int s, int X, int Y, int W, int H, boolean r, String P)
    {
      super(s, X, Y, W, H, r, 100, 1, P);
    }

    public void act()
    {
      if (PicName.substring(PicName.length()-4, PicName.length()).equals("side"))
      {
        if (facing)
          x += 2 * speed;
        else
          x -= speed;

        if (x >= 0)
          facing = false;

      }
      else if (PicName.substring(PicName.length()-2, PicName.length()).equals("up"))
      {
        if (facing)
          y += speed;
        else
          y -= 2 * speed;

        if (600 - y >= h)
          facing = true;
      }
    }
  }

  class Item
  {
    int x, y, mag;
    String des;

    public Item(String description, int X, int Y, int magnitude)
    {
      des = description;
      x = X;
      y = Y;
      mag = magnitude;
    }

    public void setCoordinates(int newx, int newy)
    {
      x = newx;
      y = newy;
    }

    public void setDes(String D)
    {
      des = D;
    }

    public void setMag(int magnitude)
    {
      mag = magnitude;
    }

    public int getX()
    {
      return x;
    }

    public int getY()
    {
      return y;
    }

    public String getDes()
    {
      return des;
    }

    public int getMag()
    {
      return mag;
    }
  }

} 
