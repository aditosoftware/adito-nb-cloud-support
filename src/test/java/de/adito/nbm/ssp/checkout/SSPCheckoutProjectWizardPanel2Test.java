package de.adito.nbm.ssp.checkout;

import de.adito.aditoweb.nbm.nbide.nbaditointerface.git.*;
import de.adito.aditoweb.nbm.nbide.nbaditointerface.git.exceptions.AditoVersioningException;
import de.adito.notification.internal.NotificationFacadeTestUtil;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.openide.util.Lookup;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;

/**
 * Test class for {@link SSPCheckoutProjectWizardPanel2}.
 *
 * @author r.hartinger, 22.02.2023
 */
class SSPCheckoutProjectWizardPanel2Test
{

  /**
   * Tests the method {@link SSPCheckoutProjectWizardPanel2#getAvailableRef(String)}.
   */
  @Nested
  class GetAvailableRef
  {

    /**
     * Tests that a thrown {@link AditoVersioningException} will be written on error in the facade.
     */
    @SneakyThrows
    @Test
    void shouldHandleAditoVersioningException()
    {
      IGitVersioningSupport gitVersioningSupport = Mockito.spy(IGitVersioningSupport.class);
      Mockito.doThrow(new AditoVersioningException("junit")).when(gitVersioningSupport).getTagsInRepository(any());
      NotificationFacadeTestUtil.verifyNotificationFacade(AditoVersioningException.class, "Could not retrieve the list of branches, check your credentials and see the IDE log for further details",
                                                          () -> baseGetAvailableRef(gitVersioningSupport, pSSPCheckoutProjectWizardPanel2 ->
                                                              assertArrayEquals(new IRef[0], pSSPCheckoutProjectWizardPanel2.getAvailableRef("myGitUrl"))));
    }

    /**
     * Tests that a normal method call with no exceptions thrown will work.
     */
    @SneakyThrows
    @Test
    void shouldGetAvailableRef()
    {
      ITag tag = Mockito.spy(ITag.class);
      IRemoteBranch remoteBranch = Mockito.spy(IRemoteBranch.class);

      IGitVersioningSupport gitVersioningSupport = Mockito.spy(IGitVersioningSupport.class);
      Mockito.doReturn(List.of(tag)).when(gitVersioningSupport).getTagsInRepository(any());
      Mockito.doReturn(List.of(remoteBranch)).when(gitVersioningSupport).getBranchesInRepository(any());
      NotificationFacadeTestUtil.verifyNoInteractionsWithNotificationFacade(
          () -> baseGetAvailableRef(gitVersioningSupport, pSSPCheckoutProjectWizardPanel2 ->
              assertArrayEquals(new IRef[]{remoteBranch, tag}, pSSPCheckoutProjectWizardPanel2.getAvailableRef("myGitUrl"))));
    }

    /**
     * Base method for testing {@link SSPCheckoutProjectWizardPanel2#getAvailableRef(String)}.
     *
     * @param gitVersioningSupport the {@link IGitVersioningSupport} that should be returned by the lookup
     * @param pAction              the action which should do the method call
     */
    private void baseGetAvailableRef(@NotNull IGitVersioningSupport gitVersioningSupport, @NotNull Consumer<SSPCheckoutProjectWizardPanel2> pAction)
    {
      SSPCheckoutProjectWizardPanel2 sspCheckoutProjectWizardPanel2 = new SSPCheckoutProjectWizardPanel2();

      try (MockedStatic<Lookup> lookupMockedStatic = Mockito.mockStatic(Lookup.class))
      {
        Lookup lookup = Mockito.mock(Lookup.class);
        Mockito.doReturn(gitVersioningSupport).when(lookup).lookup(IGitVersioningSupport.class);

        lookupMockedStatic.when(Lookup::getDefault).thenReturn(lookup);

        pAction.accept(sspCheckoutProjectWizardPanel2);
      }
    }
  }

}
