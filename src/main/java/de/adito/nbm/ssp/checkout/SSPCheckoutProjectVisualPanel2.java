package de.adito.nbm.ssp.checkout;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.git.*;
import de.adito.nbm.ssp.checkout.SSPCheckoutProjectWizardIterator.ECheckoutMode;
import de.adito.swing.TableLayoutUtil;
import info.clearthought.layout.TableLayout;
import lombok.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

import static de.adito.nbm.ssp.checkout.SSPCheckoutProjectWizardIterator.getMessage;

/**
 * The visualPanel of the wizard in which the location of the project to create are entered
 *
 * @author s.danner, 13.09.13
 */
public class SSPCheckoutProjectVisualPanel2 extends JPanel
{
  private JButton projectLocationBrowseButton;
  private JTextField projectNameTextField;
  private JTextField projectLocationTextField;
  private List<CheckoutModeRadioButton> checkoutModeSelectionButtons;
  private JComboBox<IRef> gitBranchComboBox;
  private JLabel gitBranchNameLabel;

  public SSPCheckoutProjectVisualPanel2()
  {
    initComponents();
  }

  /**
   * intiates the visual components
   */
  private void initComponents()
  {
    projectLocationTextField = new JTextField();
    projectNameTextField = new JTextField();

    checkoutModeSelectionButtons = List.of(
        new CheckoutModeRadioButton(ECheckoutMode.PROJECT_ONLY, getMessage(SSPCheckoutProjectVisualPanel2.class,
                                                                           "SSPCheckoutProjectVisualPanel2.checkoutmode.project_only.text"), true),
        new CheckoutModeRadioButton(ECheckoutMode.SYSTEMSTATE_ONLY, getMessage(SSPCheckoutProjectVisualPanel2.class,
                                                                               "SSPCheckoutProjectVisualPanel2.checkoutmode.systemstate_only.text"), false),
        new CheckoutModeRadioButton(ECheckoutMode.PROJECT_AND_SYSTEMSTATE, getMessage(SSPCheckoutProjectVisualPanel2.class,
                                                                                      "SSPCheckoutProjectVisualPanel2.checkoutmode.project_and_systemstate.text"), false)
    );

    JLabel projectNameLabel = new JLabel(getMessage(SSPCheckoutProjectVisualPanel2.class,
                                                    "SSPCheckoutProjectVisualPanel2.projectNameLabel.text"));
    JLabel projectLocationLabel = new JLabel(getMessage(SSPCheckoutProjectVisualPanel2.class,
                                                        "SSPCheckoutProjectVisualPanel2.projectLocationLabel.text"));
    JLabel checkoutModeSelectionLabel = new JLabel("Checkout Mode");

    projectLocationBrowseButton = new JButton(getMessage(SSPCheckoutProjectVisualPanel2.class,
                                                         "SSPCheckoutProjectVisualPanel2.projectLocationBrowseButton.text"));
    projectLocationTextField.setText(getMessage(SSPCheckoutProjectVisualPanel2.class,
                                                "SSPCheckoutProjectVisualPanel2.projectLocationTextField.text"));
    gitBranchNameLabel = new JLabel("Branch/Tag");
    gitBranchComboBox = new JComboBox<>();
    gitBranchComboBox.setRenderer(createListRenderer());

    int compGap = 10;
    int layoutGap = 18;
    TableLayout tlu = new TableLayout(new double[]{TableLayout.PREFERRED, layoutGap, TableLayout.FILL}, new double[]{
        TableLayout.PREFERRED,
        layoutGap,
        TableLayout.PREFERRED,
        layoutGap,
        TableLayout.PREFERRED,
        layoutGap,
        TableLayout.PREFERRED,
        });
    setLayout(tlu);
    setBorder(new EmptyBorder(compGap, compGap, compGap, compGap));
    setPreferredSize(new Dimension(700, 400));

    TableLayoutUtil util = new TableLayoutUtil(this);

    // Project Name
    util.add(0, 0, projectNameLabel);
    util.add(2, 0, projectNameTextField);

    // Project Location
    JPanel locationPanel = new JPanel(new BorderLayout(compGap, 0));
    locationPanel.add(projectLocationTextField, BorderLayout.CENTER);
    locationPanel.add(projectLocationBrowseButton, BorderLayout.EAST);
    util.add(0, 2, projectLocationLabel);
    util.add(2, 2, locationPanel);

    // Checkout Mode
    ButtonGroup checkoutModeSelectionButtonGroup = new ButtonGroup();
    JPanel checkoutModeSelectionButtonPanel = new JPanel();
    checkoutModeSelectionButtonPanel.setLayout(new BoxLayout(checkoutModeSelectionButtonPanel, BoxLayout.Y_AXIS));
    for (int i = 0; i < checkoutModeSelectionButtons.size(); i++)
    {
      CheckoutModeRadioButton button = checkoutModeSelectionButtons.get(i);
      checkoutModeSelectionButtonGroup.add(button);
      if (i > 0)
        checkoutModeSelectionButtonPanel.add(Box.createVerticalStrut(compGap));
      checkoutModeSelectionButtonPanel.add(button);
    }
    util.add(0, 4, checkoutModeSelectionLabel);
    util.add(2, 4, checkoutModeSelectionButtonPanel);

    // Branch/Tag
    util.add(0, 6, gitBranchNameLabel);
    util.add(2, 6, gitBranchComboBox);
    checkoutModeSelectionButtons.forEach(pBtn -> {
      if (pBtn.isSelected())
        gitBranchComboBox.setEnabled(pBtn.getCheckoutMode().isLoadProject());
      pBtn.addActionListener(a -> gitBranchComboBox.setEnabled(pBtn.getCheckoutMode().isLoadProject()));
    });
  }

  /**
   * Returns the name of the step
   *
   * @return name as string
   */
  @Override
  public String getName()
  {
    return getMessage(SSPCheckoutProjectVisualPanel2.class, "SSPCheckoutProjectVisualPanel2.stepName.text");
  }

  public JButton getProjectLocationBrowseButton()
  {
    return projectLocationBrowseButton;
  }

  public JTextField getProjectLocationTextField()
  {
    return projectLocationTextField;
  }

  public JTextField getProjectNameTextField()
  {
    return projectNameTextField;
  }

  public JComboBox<IRef> getGitBranchComboBox()
  {
    return gitBranchComboBox;
  }

  public JLabel getGitBranchNameLabel()
  {
    return gitBranchNameLabel;
  }

  /**
   * @return all available checkout mode radio buttons
   */
  @NonNull
  public List<CheckoutModeRadioButton> getCheckoutModeSelectionButtons()
  {
    return checkoutModeSelectionButtons;
  }

  private static DefaultListCellRenderer createListRenderer()
  {
    return new DefaultListCellRenderer()
    {

      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                    boolean isSelected, boolean cellHasFocus)
      {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value != null)
        {
          if (value instanceof ITag)
          {
            ITag tag = (ITag) value;
            this.setText("Tag: " + tag.getName());
          }
          if (value instanceof IRemoteBranch)
          {
            IRemoteBranch branch = (IRemoteBranch) value;
            this.setText("Branch: " + branch.getName());
          }
        }
        return this;
      }
    };
  }

  /**
   * RadioButtons to display a specific {@link ECheckoutMode}
   */
  public static class CheckoutModeRadioButton extends JRadioButton
  {
    @Getter
    @NonNull
    private final ECheckoutMode checkoutMode;

    public CheckoutModeRadioButton(@NonNull ECheckoutMode pCheckoutMode, @NonNull String pText, boolean pIsSelected)
    {
      super(pText, pIsSelected);
      setActionCommand(pCheckoutMode.name());
      checkoutMode = pCheckoutMode;
    }

    /**
     * Updates the enabled state based on the given project availability.
     *
     * @param pHasProject true, if a git project is currently available
     */
    public void updateEnabledBasedOnProjectAvailability(boolean pHasProject)
    {
      setEnabled(pHasProject || !checkoutMode.isLoadProject());
    }
  }

}
