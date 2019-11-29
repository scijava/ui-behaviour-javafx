/*-
 * #%L
 * Configurable key and mouse event handling
 * %%
 * Copyright (C) 2019 Tobias Pietzsch
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.scijava.ui.behaviour.javafx;

import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import org.scijava.ui.behaviour.AbstractMouseAndKeyHandler;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.DragBehaviour;
import org.scijava.ui.behaviour.InputTrigger;
import org.scijava.ui.behaviour.KeyPressedManager;
import org.scijava.ui.behaviour.KeyPressedManager.KeyPressedReceiver;
import org.scijava.ui.behaviour.ScrollBehaviour;

public class JfxMouseAndKeyHandler extends AbstractMouseAndKeyHandler
		implements EventHandler< Event >
{
	/**
	 * Which keys are currently pressed. This does not include modifier keys
	 * Control, Shift, Alt, AltGr, Meta, Win.
	 */
	private final TIntSet pressedKeys = new TIntHashSet( 5, 0.5f, -1 );

	/**
	 * When keys where pressed
	 */
	private final TIntLongHashMap keyPressTimes = new TIntLongHashMap( 100, 0.5f, -1, -1 );

	/**
	 * Whether the WINDOWS key is currently pressed.
	 */
	private boolean winPressed = false;

	/**
	 * Whether the ALT_GRAPH key is currently pressed.
	 */
	private boolean altGraphPressed = false;

	/**
	 * The current mouse coordinates, updated through {@link #mouseMoved(MouseEvent)}.
	 */
	private int mouseX;

	/**
	 * The current mouse coordinates, updated through {@link #mouseMoved(MouseEvent)}.
	 */
	private int mouseY;

	/**
	 * Active {@link DragBehaviour}s initiated by mouse button press.
	 */
	private final ArrayList< BehaviourEntry< DragBehaviour > > activeButtonDrags = new ArrayList<>();

	/**
	 * Active {@link DragBehaviour}s initiated by key press.
	 */
	private final ArrayList< BehaviourEntry< DragBehaviour > > activeKeyDrags = new ArrayList<>();

	@Override
	public void handle( final Event event )
	{
		if ( event instanceof InputEvent )
		{
			update();

			if ( event instanceof MouseEvent )
			{
				final EventType< ? extends Event > type = event.getEventType();
				if ( type == MouseEvent.MOUSE_PRESSED )
					mousePressed( ( MouseEvent ) event );
				if ( type == MouseEvent.MOUSE_DRAGGED )
					mouseDragged( ( MouseEvent ) event );
				else if ( type == MouseEvent.MOUSE_RELEASED )
					mouseReleased( ( MouseEvent ) event );
				else if ( type == MouseEvent.MOUSE_CLICKED )
					mouseClicked( ( MouseEvent ) event );
				else if ( type == MouseEvent.MOUSE_MOVED )
					mouseMoved( ( MouseEvent ) event );
				else if ( type == MouseEvent.MOUSE_ENTERED )
					mouseEntered();
				else if ( type == MouseEvent.MOUSE_EXITED )
					mouseExited();
			}
			else if ( event instanceof KeyEvent )
			{
				final EventType< ? extends Event > type = event.getEventType();
				if ( type == KeyEvent.KEY_PRESSED )
					keyPressed( ( KeyEvent ) event );
				else if ( type == KeyEvent.KEY_RELEASED )
					keyReleased( ( KeyEvent ) event );
				// TODO: Use KeyEvent.KEY_TYPED for InputMap/ActionMap equivalent?
			}
			else if ( event instanceof ScrollEvent )
			{
				scrolled( ( ScrollEvent ) event );
			}
		}
	}

	public void scrolled( final ScrollEvent e )
	{
		final int mask = getMask( e );
		final int x = ( int ) e.getX();
		final int y = ( int ) e.getY();

		final double dX = e.getDeltaX();
		final double dY = e.getDeltaY();
		final boolean isHorizontal = Math.abs( dX ) > Math.abs( dY );
		final double amount = isHorizontal ? dX : dY;

		for ( final BehaviourEntry< ScrollBehaviour > scroll : scrolls )
			if ( scroll.buttons().matches( mask, pressedKeys ) )
				scroll.behaviour().scroll( amount, isHorizontal, x, y );
	}

	private void mouseDragged( final MouseEvent e )
	{
		mouseX = ( int ) e.getX();
		mouseY = ( int ) e.getY();

		for ( final BehaviourEntry< DragBehaviour > drag : activeButtonDrags )
			drag.behaviour().drag( mouseX, mouseY );
	}

	private void mouseMoved( final MouseEvent e )
	{
		mouseX = ( int ) e.getX();
		mouseY = ( int ) e.getY();

		for ( final BehaviourEntry< DragBehaviour > drag : activeKeyDrags )
			drag.behaviour().drag( mouseX, mouseY );
	}

	private void mouseClicked( final MouseEvent e )
	{
		final int mask = getMask( e );
		final int x = ( int ) e.getX();
		final int y = ( int ) e.getY();

		final int clickMask = mask & ~InputTrigger.DOUBLE_CLICK_MASK;
		for ( final BehaviourEntry< ClickBehaviour > click : buttonClicks )
		{
			if ( click.buttons().matches( mask, pressedKeys ) ||
					( clickMask != mask && click.buttons().matches( clickMask, pressedKeys ) ) )
			{
				click.behaviour().click( x, y );
			}
		}
	}

	private void mousePressed( final MouseEvent e )
	{
		final int mask = getMask( e );
		final int x = ( int ) e.getX();
		final int y = ( int ) e.getY();

		for ( final BehaviourEntry< DragBehaviour > drag : buttonDrags )
		{
			if ( drag.buttons().matches( mask, pressedKeys ) )
			{
				drag.behaviour().init( x, y );
				activeButtonDrags.add( drag );
			}
		}
	}

	private void mouseReleased( final MouseEvent e )
	{
		final int mask = getMask( e );
		final int x = ( int ) e.getX();
		final int y = ( int ) e.getY();

		final ArrayList< BehaviourEntry< ? > > ended = new ArrayList<>();
		for ( final BehaviourEntry< DragBehaviour > drag : activeButtonDrags )
			if ( !drag.buttons().matchesSubset( mask, pressedKeys ) )
			{
				drag.behaviour().end( x, y );
				ended.add( drag );
			}
		activeButtonDrags.removeAll( ended );
	}

	private void mouseEntered()
	{
		if ( keypressManager != null )
			keypressManager.activate( receiver );
	}

	private void mouseExited()
	{
		if ( keypressManager != null )
			keypressManager.deactivate( receiver );
	}

	private void keyPressed( final KeyEvent e )
	{
		final KeyCode code = e.getCode();

		if ( code == KeyCode.WINDOWS )
			this.winPressed = true;
		else if ( code == KeyCode.ALT_GRAPH )
			this.altGraphPressed = true;
		else if ( code != KeyCode.UNDEFINED &&
				code != KeyCode.SHIFT &&
				code != KeyCode.META &&
				code != KeyCode.COMMAND &&
				code != KeyCode.ALT &&
				code != KeyCode.CONTROL )
		{
			final int key = getKeyCode( code );
			final boolean inserted = pressedKeys.add( key );

			/*
			 * Create mask and deal with double-click on keys.
			 */

			final int mask = getMask( e );
			boolean doubleClick = false;
			if ( inserted )
			{
				// double-click on keys.
				final long time = System.currentTimeMillis();
				final long lastPressTime = keyPressTimes.get( key );
				if ( lastPressTime != -1 && ( time - lastPressTime ) < DOUBLE_CLICK_INTERVAL )
					doubleClick = true;

				keyPressTimes.put( key, time );
			}

			if ( keypressManager != null )
				keypressManager.handleKeyPressed( receiver, mask, doubleClick, pressedKeys );
			else
				handleKeyPressed( mask, doubleClick, pressedKeys, false );
		}
	}

	/**
	 * If non-null, {@code keyPressed()} events are forwarded to the
	 * {@link KeyPressedManager} which in turn forwards to the
	 * {@link KeyPressedReceiver} of the component currently under the mouse.
	 * (This requires that the other component is also registered with the
	 * {@link KeyPressedManager}.
	 */
	private KeyPressedManager keypressManager = null;

	/**
	 * Represents this {@link JfxMouseAndKeyHandler} to the {@link #keypressManager}.
	 */
	private KeyPressedReceiver receiver = null;

	private void transferKeysTo( JfxMouseAndKeyHandler target )
	{
		target.pressedKeys.clear();
		target.pressedKeys.addAll( pressedKeys );
		target.keyPressTimes.clear();
		target.keyPressTimes.putAll( keyPressTimes );
		target.winPressed = winPressed;
		target.altGraphPressed = altGraphPressed;

		pressedKeys.clear();
		keyPressTimes.clear();
		winPressed = false;
		altGraphPressed = false;
	}

	private static class KeyPressedReceiverImp implements KeyPressedReceiver
	{
		private final JfxMouseAndKeyHandler handler;

		private final Runnable focus;

		KeyPressedReceiverImp( final JfxMouseAndKeyHandler handler, final Runnable focus )
		{
			this.handler = handler;
			this.focus = focus;
		}

		@Override
		public void handleKeyPressed( final KeyPressedReceiver origin, final int mask, final boolean doubleClick, final TIntSet pressedKeys )
		{
			if ( handler.handleKeyPressed( mask, doubleClick, pressedKeys, true ) )
			{
				if ( origin instanceof KeyPressedReceiverImp )
				{
					final KeyPressedReceiverImp o = ( KeyPressedReceiverImp ) origin;
					if ( o.handler != this.handler )
						o.handler.transferKeysTo( this.handler );
				}
				focus.run();
				handler.handleKeyPressed( mask, doubleClick, pressedKeys, false );
			}
		}
	}

	/**
	 * @param keypressManager
	 * @param focus
	 *            function that ensures that the component associated to this
	 *            {@link JfxMouseAndKeyHandler} is focused.
	 */
	public void setKeypressManager(
			final KeyPressedManager keypressManager,
			final Runnable focus )
	{
		this.keypressManager = keypressManager;
		this.receiver = new KeyPressedReceiverImp( this, focus );
	}

	private boolean handleKeyPressed( final int mask, final boolean doubleClick, final TIntSet pressedKeys, final boolean dryRun )
	{
		update();

		final int doubleClickMask = mask | InputTrigger.DOUBLE_CLICK_MASK;

		boolean triggered = false;

		for ( final BehaviourEntry< DragBehaviour > drag : keyDrags )
		{
			if ( !activeKeyDrags.contains( drag ) &&
					( drag.buttons().matches( mask, pressedKeys ) ||
							( doubleClick && drag.buttons().matches( doubleClickMask, pressedKeys ) ) ) )
			{
				if ( dryRun )
					return true;
				triggered = true;
				drag.behaviour().init( mouseX, mouseY );
				activeKeyDrags.add( drag );
			}
		}

		for ( final BehaviourEntry< ClickBehaviour > click : keyClicks )
		{
			if ( click.buttons().matches( mask, pressedKeys ) ||
					( doubleClick && click.buttons().matches( doubleClickMask, pressedKeys ) ) )
			{
				if ( dryRun )
					return true;
				triggered = true;
				click.behaviour().click( mouseX, mouseY );
			}
		}

		return triggered;
	}

	private void keyReleased( final KeyEvent e )
	{
		final KeyCode code = e.getCode();
		if ( code == KeyCode.WINDOWS )
			this.winPressed = true;
		else if ( code == KeyCode.ALT_GRAPH )
			this.altGraphPressed = true;
		else if ( code != KeyCode.UNDEFINED &&
				code != KeyCode.SHIFT &&
				code != KeyCode.META &&
				code != KeyCode.COMMAND &&
				code != KeyCode.ALT &&
				code != KeyCode.CONTROL )
		{
			final int key = getKeyCode( code );
			pressedKeys.remove( key );
			final int mask = getMask( e );

			final ArrayList< BehaviourEntry< ? > > ended = new ArrayList<>();
			for ( final BehaviourEntry< DragBehaviour > drag : activeKeyDrags )
				if ( !drag.buttons().matchesSubset( mask, pressedKeys ) )
				{
					drag.behaviour().end( mouseX, mouseY );
					ended.add( drag );
				}
			activeKeyDrags.removeAll( ended );
		}
	}

	private int getMask( final MouseEvent e )
	{
		int mask = 0;

		if ( e.isShiftDown() )
			mask |= InputTrigger.SHIFT_DOWN_MASK;
		if ( e.isControlDown() )
			mask |= InputTrigger.CTRL_DOWN_MASK;
		if ( e.isMetaDown() )
			mask |= InputTrigger.META_DOWN_MASK;
		if ( e.isAltDown() )
			mask |= InputTrigger.ALT_DOWN_MASK;
		if ( this.isAltGraphDown() )
			mask |= InputTrigger.ALT_GRAPH_DOWN_MASK;
		if ( this.isWinDown() )
			mask |= InputTrigger.WIN_DOWN_MASK;


		if ( e.isPrimaryButtonDown() )
			mask |= InputTrigger.BUTTON1_DOWN_MASK;
		if ( e.isMiddleButtonDown() )
			mask |= InputTrigger.BUTTON2_DOWN_MASK;
		if ( e.isSecondaryButtonDown() )
			mask |= InputTrigger.BUTTON3_DOWN_MASK;

		/*
		 * We add the button modifiers to modifiersEx such that the
		 * XXX_DOWN_MASK can be used as the canonical flag. E.g. we adapt
		 * mask such that BUTTON1_DOWN_MASK is also present in
		 * mouseClicked() when BUTTON1 was clicked (although the button is no
		 * longer down at this point).
		 */
		if ( e.getEventType() == MouseEvent.MOUSE_CLICKED )
		{
			switch ( e.getButton() )
			{
			case PRIMARY:
				mask |= InputTrigger.BUTTON1_DOWN_MASK;
				break;
			case MIDDLE:
				mask |= InputTrigger.BUTTON2_DOWN_MASK;
				break;
			case SECONDARY:
				mask |= InputTrigger.BUTTON3_DOWN_MASK;
				break;
			}
		}

		if ( e.getClickCount() > 1 )
			mask |= InputTrigger.DOUBLE_CLICK_MASK;

		return mask;
	}

	private int getMask( final ScrollEvent e )
	{
		int mask = InputTrigger.SCROLL_MASK;

		if ( e.isShiftDown() )
			mask |= InputTrigger.SHIFT_DOWN_MASK;
		if ( e.isControlDown() )
			mask |= InputTrigger.CTRL_DOWN_MASK;
		if ( e.isMetaDown() )
			mask |= InputTrigger.META_DOWN_MASK;
		if ( e.isAltDown() )
			mask |= InputTrigger.ALT_DOWN_MASK;
		if ( this.isAltGraphDown() )
			mask |= InputTrigger.ALT_GRAPH_DOWN_MASK;
		if ( this.isWinDown() )
			mask |= InputTrigger.WIN_DOWN_MASK;

		return mask;
	}

	private int getMask( final KeyEvent e )
	{
		int mask = 0;

		if ( e.isShiftDown() )
			mask |= InputTrigger.SHIFT_DOWN_MASK;
		if ( e.isControlDown() )
			mask |= InputTrigger.CTRL_DOWN_MASK;
		if ( e.isMetaDown() )
			mask |= InputTrigger.META_DOWN_MASK;
		if ( e.isAltDown() )
			mask |= InputTrigger.ALT_DOWN_MASK;
		if ( this.isAltGraphDown() )
			mask |= InputTrigger.ALT_GRAPH_DOWN_MASK;
		if ( this.isWinDown() )
			mask |= InputTrigger.WIN_DOWN_MASK;

		return mask;
	}

	private boolean isWinDown()
	{
		return winPressed;
	}

	private boolean isAltGraphDown()
	{
		return altGraphPressed;
	}

	private final static Method getKeyCode;

	static
	{
		Method m = null;
		try
		{
			m = KeyCode.class.getDeclaredMethod( "getCode" ); // Java 9+
		}
		catch ( NoSuchMethodException e )
		{
			try
			{
				m = KeyCode.class.getDeclaredMethod( "impl_getCode" ); // Java 8
			}
			catch ( NoSuchMethodException ex )
			{
				throw new RuntimeException( ex );
			}
		}
		getKeyCode = m;
	}

	private int getKeyCode( KeyCode keyCode )
	{
		try
		{
			return ( int ) getKeyCode.invoke( keyCode );
		}
		catch ( IllegalAccessException | InvocationTargetException e )
		{
			throw new RuntimeException( e );
		}
	}
}
