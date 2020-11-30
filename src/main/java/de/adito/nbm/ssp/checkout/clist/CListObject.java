package de.adito.nbm.ssp.checkout.clist;


import com.google.common.collect.Lists;
import com.google.common.graph.Traverser;
import de.adito.nbm.ssp.checkout.SSPCheckoutProjectWizardIterator;
import de.adito.nbm.ssp.facade.ISSPSystemDetails;
import org.openide.util.ImageUtilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Represents an object of the CList whose proeprties are determined by ISSPSystemDetails
 *
 * @author W.Glanzer, 10.09.13
 */
public class CListObject extends JPanel
{
  public static final int MAX_HEIGHT = 110;

  private final ISSPSystemDetails systemDetails;
  private JTextArea lblShortDesc;
  private boolean enabled = true;

  /**
   * - initiates the variables
   * - configures the object (Max size, background color, mouse listener)
   *
   * @param pDescription - ISSPSystemDetails with all information about the system
   */
  public CListObject(ISSPSystemDetails pDescription)
  {
    systemDetails = pDescription;
    setPreferredSize(new Dimension((int) getMaximumSize().getWidth(), MAX_HEIGHT));
    setOpaque(true);
    setBackground(SSPCheckoutProjectWizardIterator.getCallback().getDefaultBackgroundColor());
    setLayout(new GridBagLayout());
    _initComponents();
    setColor(SSPCheckoutProjectWizardIterator.getCallback().getDefaultBackgroundColor());
    setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.black));

    Traverser<Component> componentTraverser = Traverser.forTree(pComponent -> {
      if (pComponent instanceof Container)
        return Lists.newArrayList(((Container) pComponent).getComponents());
      return null;
    });
    componentTraverser.breadthFirst(this).forEach(pComponent -> pComponent.setFocusable(false));
  }

  /**
   * initiates all components of the CListObject
   */
  private void _initComponents()
  {
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.HORIZONTAL;

    CListObjectPicture picture = new CListObjectPicture(ImageUtilities.loadImage("de/adito/nbm/ssp/checkout/clist/systemImage.png"));
    picture.setOpaque(false);

    JLabel lblTitle = new JLabel(systemDetails.getName());
    lblTitle.setFont(new Font("default", Font.BOLD, 12));
    lblTitle.setOpaque(false);

    String dateCreated = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        .withZone(ZoneId.systemDefault())
        .format(systemDetails.getCreationDate());

    lblShortDesc = new JTextArea("Git repo: " + systemDetails.getGitRepoUrl() + "\nGit branch: " + systemDetails.getGitBranch()
                                     + "\nKernel version: " + systemDetails.getKernelVersion() + "\nDate created: " + dateCreated);
    lblShortDesc.setLineWrap(true);
    lblShortDesc.setWrapStyleWord(true);
    lblShortDesc.setEditable(false);
    lblShortDesc.setOpaque(false);
    lblShortDesc.setBackground(new Color(0, 0, 0, 0));
    lblShortDesc.setCaret(new CListCaretAdapter());

    c.insets = new Insets(0, 10, 0, 10);
    c.anchor = GridBagConstraints.CENTER;
    c.gridx = 0;
    c.gridy = 0;
    c.weightx = 0;
    c.weighty = 0;
    add(picture, c);

    JPanel rightSide = new JPanel();
    rightSide.setLayout(new BorderLayout(0, 5));

    rightSide.add(lblTitle, BorderLayout.NORTH);
    rightSide.add(lblShortDesc, BorderLayout.CENTER);
    rightSide.setOpaque(false);

    c.insets = new Insets(10, 0, 10, 0);
    c.fill = GridBagConstraints.BOTH;
    c.gridx = 1;
    c.gridy = 0;
    c.weightx = 1;
    c.weighty = 1;
    add(rightSide, c);
  }

  /**
   * Disables this list entry if the designer project version is too low for the project
   */
  public void disableProject()
  {
    enabled = false;
    _disableComponents(this);
    //String requiredVersion = AditoOnlineProjectWizardIterator.getMessage(this, "CListObject.version");
    //String currentVersion = AditoOnlineProjectWizardIterator.getMessage(this, "CListObject.currentVersion");
    //String designerVersion;
    //try
    //{
    //  designerVersion = new Version(IVersion.DESIGNER).toString();
    //}
    //catch (AditoIllegalArgumentException e)
    //{
    //  throw new AditoRuntimeException(e, 20, 680);
    //}
    //
    //JTextArea info = new JTextArea(currentVersion + " " + designerVersion + "\n" +
    //                               requiredVersion + " " + systemDetails.getMinProjectVersion());
    //info.setForeground(Color.red);

    GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(10, 20, 0, 0);
    c.anchor = GridBagConstraints.SOUTHEAST;
    c.gridx = 2;
    c.gridy = 0;
    c.weightx = 0;
    c.weighty = 0;

    //add(info, c);
  }

  /**
   * Disables all graphical components of a container recursively until none are left
   *
   * @param pContainer Containr that contains the component
   */
  private void _disableComponents(Container pContainer)
  {
    Component[] components = pContainer.getComponents();
    for (Component component : components)
    {
      component.setEnabled(false);
      if (component instanceof Container)
      {
        _disableComponents((Container) component);
      }
    }
  }

  /**
   * Adds a mouseAdapter
   *
   * @param pAdapter MouseAdapter that should be added
   */
  public void addMouseAdapter(MouseAdapter pAdapter)
  {
    if (!enabled)
      return;

    addMouseListener(pAdapter);
  }

  /**
   * Sets a MouseAdapter on the shortDescription field of the object
   *
   * @param pMouseAdapter - MouseAdapter that should be set
   */
  public void addMouseAdapterOnTextField(final MouseAdapter pMouseAdapter)
  {
    if (!enabled)
      return;

    if (lblShortDesc != null)
      lblShortDesc.addMouseListener(pMouseAdapter);
    else
      SwingUtilities.invokeLater(() -> lblShortDesc.addMouseListener(pMouseAdapter));
  }

  /**
   * Sets the backgroundColor of all components of the object
   *
   * @param pColor - background color that the object should have
   */
  public void setColor(final Color pColor)
  {
    SwingUtilities.invokeLater(() -> setBackground(pColor));
  }

  /**
   * Returns the ISSPSystemDetails that serve as basis of the CListObject
   *
   * @return ISSPSystemDetails
   */
  public ISSPSystemDetails getSystemDetails()
  {
    return systemDetails;
  }
}
