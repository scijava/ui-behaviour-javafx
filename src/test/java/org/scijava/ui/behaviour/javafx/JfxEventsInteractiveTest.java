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
import org.scijava.ui.behaviour.InputTrigger;
import org.scijava.ui.behaviour.InputTriggerMap;
import org.scijava.ui.behaviour.ScrollBehaviour;

public class JfxEventsInteractiveTest extends Application
{
	static class MyDragBehaviour implements DragBehaviour
	{
		private final String name;

		public MyDragBehaviour(
				final String name,
				final String inputTrigger,
				final BehaviourMap behaviourMap,
				final InputTriggerMap inputMap )
		{
			this.name = name;
			behaviourMap.put( name, this );
			inputMap.put( InputTrigger.getFromString( inputTrigger ), name );
		}

		@Override
		public void init( final int x, final int y )
		{
			System.out.println( name + ": init(" + x + ", " + y + ")" );
		}

		@Override
		public void drag( final int x, final int y )
		{
			System.out.println( name + ": drag(" + x + ", " + y + ")" );
		}

		@Override
		public void end( final int x, final int y )
		{
			System.out.println( name + ": end(" + x + ", " + y + ")" );
		}
	}

	static class MyClickBehaviour implements ClickBehaviour
	{
		private final String name;

		public MyClickBehaviour(
				final String name,
				final String inputTrigger,
				final BehaviourMap behaviourMap,
				final InputTriggerMap inputMap )
		{
			this.name = name;
			behaviourMap.put( name, this );
			inputMap.put( InputTrigger.getFromString( inputTrigger ), name );
		}

		@Override
		public void click( final int x, final int y )
		{
			System.out.println( name + ": click(" + x + ", " + y + ")" );
		}
	}

	static class MyScrollBehaviour implements ScrollBehaviour
	{
		private final String name;

		public MyScrollBehaviour(
				final String name,
				final String inputTrigger,
				final BehaviourMap behaviourMap,
				final InputTriggerMap inputMap )
		{
			this.name = name;
			behaviourMap.put( name, this );
			inputMap.put( InputTrigger.getFromString( inputTrigger ), name );
		}

		@Override
		public void scroll( final double wheelRotation, final boolean isHorizontal, final int x, final int y )
		{
			System.out.println( name + ": scroll(" + wheelRotation + ", " + isHorizontal + ", " + x + ", " + y + ")" );
		}
	}

	public static void main( final String[] args )
	{
		launch( args );
	}

	@Override
	public void start( final Stage primaryStage ) throws Exception
	{
		Group root = new Group();
		Scene scene = new Scene( root, 400, 400 );
		primaryStage.setTitle( "JfxEventsInteractiveTest" );
		primaryStage.setScene( scene );
		primaryStage.show();

		/*
		 * Disable the alt key functionality that moves the focus to the window
		 * menu on Windows platform.
		 */
		// TODO: @JYT -- is this a problem with JavaFX, too? How to solve? --


		final JfxMouseAndKeyHandler handler = new JfxMouseAndKeyHandler();
		scene.addEventFilter( Event.ANY, handler );

		final InputTriggerMap inputMap = new InputTriggerMap();
		final BehaviourMap behaviourMap = new BehaviourMap();
		handler.setInputMap( inputMap );
		handler.setBehaviourMap( behaviourMap );

		new MyDragBehaviour( "left-drag", "button1", behaviourMap, inputMap );
		new MyDragBehaviour( "meta-left-drag", "meta button1", behaviourMap, inputMap );
		new MyDragBehaviour( "shift-left-drag", "shift button1", behaviourMap, inputMap );
		new MyDragBehaviour( "ctrl-left-drag", "ctrl button1", behaviourMap, inputMap );
		new MyDragBehaviour( "alt-left-drag", "alt button1", behaviourMap, inputMap );
		new MyDragBehaviour( "win-left-drag", "win button1", behaviourMap, inputMap );

		new MyDragBehaviour( "right-drag", "button3", behaviourMap, inputMap );
		new MyDragBehaviour( "meta-right-drag", "meta button3", behaviourMap, inputMap );
		new MyDragBehaviour( "shift-right-drag", "shift button3", behaviourMap, inputMap );
		new MyDragBehaviour( "ctrl-right-drag", "ctrl button3", behaviourMap, inputMap );
		new MyDragBehaviour( "alt-right-drag", "alt button3", behaviourMap, inputMap );
		new MyDragBehaviour( "win-right-drag", "win button3", behaviourMap, inputMap );

		new MyClickBehaviour( "left-click", "button1", behaviourMap, inputMap );
		new MyClickBehaviour( "meta-left-click", "meta button1", behaviourMap, inputMap );
		new MyClickBehaviour( "shift-left-click", "shift button1", behaviourMap, inputMap );
		new MyClickBehaviour( "ctrl-left-click", "ctrl button1", behaviourMap, inputMap );
		new MyClickBehaviour( "alt-left-click", "alt button1", behaviourMap, inputMap );
		new MyClickBehaviour( "win-left-click", "win button1", behaviourMap, inputMap );

		new MyClickBehaviour( "right-click", "button3", behaviourMap, inputMap );
		new MyClickBehaviour( "meta-right-click", "meta button3", behaviourMap, inputMap );
		new MyClickBehaviour( "shift-right-click", "shift button3", behaviourMap, inputMap );
		new MyClickBehaviour( "ctrl-right-click", "ctrl button3", behaviourMap, inputMap );
		new MyClickBehaviour( "alt-right-click", "alt button3", behaviourMap, inputMap );
		new MyClickBehaviour( "win-right-click", "win button3", behaviourMap, inputMap );

		new MyScrollBehaviour( "scroll", "scroll", behaviourMap, inputMap );
		new MyScrollBehaviour( "meta-scroll", "meta scroll", behaviourMap, inputMap );
		new MyScrollBehaviour( "shift-scroll", "shift scroll", behaviourMap, inputMap );
		new MyScrollBehaviour( "ctrl-scroll", "ctrl scroll", behaviourMap, inputMap );
		new MyScrollBehaviour( "alt-scroll", "alt scroll", behaviourMap, inputMap );
		new MyScrollBehaviour( "win-scroll", "win scroll", behaviourMap, inputMap );
	}
}
