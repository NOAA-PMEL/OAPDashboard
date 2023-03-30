package gov.noaa.pmel.dashboard.client.progress.state;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import com.google.gwt.core.client.GWT;

public abstract class AbstractState implements State {

  private transient PropertyChangeSupport changes = new PropertyChangeSupport(this);

  @Override
  public final void addPropertyChangeListener(final PropertyChangeListener l) {
    changes.addPropertyChangeListener(l);
  }

  @Override
  public final void addPropertyChangeListener(final String propertyName,
          final PropertyChangeListener l) {
      GWT.log("AbsState: adding listener for " + propertyName);
    changes.addPropertyChangeListener(propertyName, l);
  }

  @Override
  public final void firePropertyChange(final String propertyName,
          final Object oldValue, final Object newValue) {
    GWT.log("AbsState: fire changed: " + propertyName + " : " + oldValue + " : " + newValue);
    changes.firePropertyChange(propertyName, oldValue, newValue);
  }

  @Override
  public final void removePropertyChangeListener(final PropertyChangeListener l) {
    changes.removePropertyChangeListener(l);
  }

  @Override
  public final void removePropertyChangeListener(final String propertyName,
          final PropertyChangeListener l) {
    changes.removePropertyChangeListener(propertyName, l);
  }
}
