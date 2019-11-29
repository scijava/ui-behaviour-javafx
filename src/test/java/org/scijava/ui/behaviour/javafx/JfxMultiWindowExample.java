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

import javafx.application.Application;
import javafx.event.Event;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.scijava.ui.behaviour.BehaviourMap;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.DragBehaviour;
import org.scijava.ui.behaviour.InputTriggerMap;
import org.scijava.ui.behaviour.KeyPressedManager;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.AbstractNamedBehaviour;
import org.scijava.ui.behaviour.util.Behaviours;

public class JfxMultiWindowExample extends Application
{
	static class MyDragBehaviour extends AbstractNamedBehaviour implements DragBehaviour
	{
		public MyDragBehaviour( final String name )
		{
			super( name );
		}

		@Override
		public void init( final int x, final int y )
		{
			System.out.println( name() + ": init(" + x + ", " + y + ")" );
		}

		@Override
		public void drag( final int x, final int y )
		{
			System.out.println( name() + ": drag(" + x + ", " + y + ")" );
		}

		@Override
		public void end( final int x, final int y )
		{
			System.out.println( name() + ": end(" + x + ", " + y + ")" );
		}
	}

	static class MyClickBehaviour extends AbstractNamedBehaviour implements ClickBehaviour
	{
		public MyClickBehaviour( final String name )
		{
			super( name );
		}

		@Override
		public void click( final int x, final int y )
		{
			System.out.println( name() + ": click(" + x + ", " + y + ")" );
		}
	}

	static class BehaviourFrame
	{
		final Stage stage;

		final Group root;

		final Scene scene;

		final String title;

		final Behaviours behaviours;

		final JfxMouseAndKeyHandler handler;

		public BehaviourFrame( final String title, final String prefix )
		{
			this.title = title;

			stage = new Stage();
			root = new Group();
			scene = new Scene( root, 400, 400 );
			stage.setTitle( title );
			stage.setScene( scene );

			/*
			 * Create InputTriggerMap and BehaviourMap. This is analogous to
			 * javax.swing InputMap and ActionMap.
			 */
			final InputTriggerMap inputMap = new InputTriggerMap();
			final BehaviourMap behaviourMap = new BehaviourMap();

			/*
			 * Create a MouseAndKeyHandler that dispatches to registered Behaviours.
			 */
			handler = new JfxMouseAndKeyHandler();
			handler.setInputMap( inputMap );
			handler.setBehaviourMap( behaviourMap );
			scene.addEventFilter( Event.ANY, handler );

			behaviours = new Behaviours( inputMap, behaviourMap, new InputTriggerConfig() );
			behaviours.namedBehaviour( new MyDragBehaviour( prefix + "drag m" ), "button1" );
			behaviours.namedBehaviour( new MyDragBehaviour( prefix + "drag k" ), "K" );
			behaviours.namedBehaviour( new MyClickBehaviour( prefix + "click c " ), "C" );

			/*
			 * Display a JPanel with the MouseAndKeyHandler registered.
			 */
			stage.show();
		}
	}

	public static void main( final String[] args )
	{
		launch( args );
	}

	@Override
	public void start( final Stage primaryStage ) throws Exception
	{
		final BehaviourFrame f1 = new BehaviourFrame( "frame1", "f1 : " );
		final BehaviourFrame f2 = new BehaviourFrame( "frame2", "f2 : " );
		f2.stage.setX( 300 );

		final KeyPressedManager manager = new KeyPressedManager();
		f1.handler.setKeypressManager( manager, f1.stage::requestFocus );
		f2.handler.setKeypressManager( manager, f2.stage::requestFocus );
	}
}
