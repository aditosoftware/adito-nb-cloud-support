package de.adito.nbm.ssp.checkout.clist;


import com.auth0.jwt.interfaces.DecodedJWT;
import com.mashape.unirest.http.exceptions.UnirestException;
import de.adito.nbm.ssp.checkout.SSPCheckoutProjectWizardIterator;
import de.adito.nbm.ssp.exceptions.*;
import de.adito.nbm.ssp.facade.*;
import org.jetbrains.annotations.NotNull;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.logging.*;

/**
 * List component
 * This component is able to display CListObjects like a normal JList
 * The list is loaded via the SSP interface
 *
 * @author W.Glanzer, 10.09.13
 */
public class CList extends JPanel implements Scrollable
{
  private final ArrayList<CListObject> objectList;  // List of all CListObjects of the CList
  private CListObject selectedObject;               // The currently selected element of the CList
  private CListObject lastSelectedObject;           // The previously seleted CListObject

  public CList()
  {
    objectList = new ArrayList<>();
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    setBackground(SSPCheckoutProjectWizardIterator.getCallback().getDefaultBackgroundColor());
    setFocusable(true);

    /*FocusListener to fix the Repainting-Bug of the JScrollPane*/
    addFocusListener(new FocusAdapter()
    {
      @Override
      public void focusGained(FocusEvent e)
      {
        _update();
      }

      @Override
      public void focusLost(FocusEvent e)
      {
        _update();
      }

      private void _update()
      {
        Container parent = getParent();
        if (parent != null && parent.getParent() instanceof JScrollPane)
        {
          JScrollPane scrollPane = (JScrollPane) parent.getParent();
          scrollPane.invalidate();
          scrollPane.repaint();
        }
      }
    });
  }

  @Override
  public Dimension getPreferredScrollableViewportSize()
  {
    return null;
  }

  @Override
  public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
  {
    return SSPCheckoutProjectWizardIterator.getCallback().getDefaultScrollSpeed();
  }

  @Override
  public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
  {
    return SSPCheckoutProjectWizardIterator.getCallback().getDefaultScrollSpeed();
  }

  @Override
  public boolean getScrollableTracksViewportWidth()
  {
    return true;
  }

  @Override
  public boolean getScrollableTracksViewportHeight()
  {
    return false;
  }

  /**
   * Fills the CList based on the data from the SSP
   *
   * @param pToken JWT used to auth with the SSP system
   * @throws UnirestException  if an error occurs during the rest call when retrieving the systems
   * @throws AditoSSPException if the response of the server contains an error status when retrieving the systems
   */
  public void fillListBasedOnURL(@NotNull DecodedJWT pToken) throws UnirestException, AditoSSPException
  {
    // clear old list
    clearList();

    List<ISSPSystemDetails> systemDetailsList = new ArrayList<>();
    ISSPFacade sspFacade = ISSPFacade.getInstance();
    List<ISSPSystem> systemList = sspFacade.getSystems(pToken.getSubject(), pToken);
    for (ISSPSystem system : systemList)
    {
      try
      {
        ISSPSystemDetails systemDetails = sspFacade.getSystemDetails(pToken.getSubject(), pToken, system);
        systemDetailsList.add(systemDetails);
      }
      catch (MalformedInputException | UnirestException | AditoSSPException pE)
      {
        // nothing, not added
      }
      catch (AditoSSPParseException pE)
      {
        Logger.getLogger(CList.class.getName()).log(Level.SEVERE, pE, () -> NbBundle.getMessage(CList.class, "ERR_ParseException"));
      }
    }
    // we want the latest system on top -> inverse sorting
    systemDetailsList.sort(Comparator.comparing(ISSPSystem::getCreationDate).reversed());

    ArrayList<CListObject> disabledProjects = new ArrayList<>();
    for (ISSPSystemDetails project : systemDetailsList)
    {
      CListObject listObject = new CListObject(project);

      if (!project.isDesignerVersionOk())
      {
        listObject.disableProject();
        disabledProjects.add(listObject);
      }
      else
        objectList.add(listObject);
    }

    // disabled-Projects at the end of the list
    objectList.addAll(disabledProjects);
    if (!objectList.isEmpty())
    {
      objectList.get(objectList.size() - 1).setBorder(null); // remove the border of the last entry
    }

    // remove the loading panel
    _removeLoading();

    // Add new objects to the panel
    for (final CListObject object : objectList)
    {
      if (Thread.interrupted())
        return;
      SwingUtilities.invokeLater(() -> add(object));
    }
    // select the first/uppermost object
    if (!objectList.isEmpty())
      setSelected(objectList.get(0));
  }

  /**
   * Sets the "Loading..." label on the clist
   */
  public void setLoading()
  {
    removeAll();
    _showMessageOnList(SSPCheckoutProjectWizardIterator.getMessage(CList.class, "CList.loading"));
  }

  /**
   * Deletes all CListObujects from the objectList
   */
  public void clearList()
  {
    objectList.clear();
    selectedObject = null;
  }

  /**
   * Remove the loading label
   */
  private void _removeLoading()
  {
    SwingUtilities.invokeLater(this::removeAll);
  }

  /**
   * Sets a certain message on the CList. Used by the "setLoading" method to display the "Loading..." message
   *
   * @param pMessage Message that should be displayed
   */
  private void _showMessageOnList(String pMessage)
  {
    JLabel message = new JLabel(pMessage);
    message.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    message.setOpaque(true);
    message.setBackground(SSPCheckoutProjectWizardIterator.getCallback().getDefaultBackgroundColor());
    add(message);
  }

  /**
   * Selects and object and colors it in a certain color
   * Sets the variable "lastSelectedObject" to the last selected element, which is returned to its original background color
   *
   * @param pSelected The newly selected object
   */
  public void setSelected(@NotNull CListObject pSelected)
  {
    if (!pSelected.getSystemDetails().isDesignerVersionOk())
      return;

    if (selectedObject != null)
    {
      SwingUtilities.invokeLater(() -> {
        if (lastSelectedObject != null)
          lastSelectedObject.setColor(SSPCheckoutProjectWizardIterator.getCallback().getDefaultBackgroundColor());
      });
    }
    lastSelectedObject = selectedObject;
    selectedObject = pSelected;
    SwingUtilities.invokeLater(() -> {
      if (selectedObject != null)
        selectedObject.setColor(SSPCheckoutProjectWizardIterator.getCallback().getDefaultBackgroundColor().darker());
    });
  }

  /**
   * Returns a list of all ClistObjects in the CList
   *
   * @return List of elements in the CList
   */
  public List<CListObject> getObjectList()
  {
    return objectList;
  }

  /**
   * @return the currently selected ClistObject
   */
  public CListObject getSelected()
  {
    return selectedObject;
  }

  /**
   * @return calculates the height of the currently selected object, used for the scrollPane
   */
  public int getScrollValue()
  {
    return (objectList.indexOf(selectedObject) - 1) * CListObject.MAX_HEIGHT;
  }

}
