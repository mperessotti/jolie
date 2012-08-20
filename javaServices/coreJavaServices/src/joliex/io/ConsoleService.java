/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/

package joliex.io;


import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.channels.Channels;
import java.nio.channels.ClosedByInterruptException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import jolie.net.CommMessage;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.embedding.RequestResponse;

public class ConsoleService extends JavaService
{
	private Map< String, String > sessionTokens;
	private boolean sessionListeners = false;

	private class ConsoleInputThread extends Thread
	{
		private boolean keepRun = true;
		
		public void kill()
		{
			keepRun = false;
			this.interrupt();
		}

		@Override
		public void run()
		{
			BufferedReader stdin =
				new BufferedReader(
						new InputStreamReader(
							Channels.newInputStream(
								(new FileInputStream( FileDescriptor.in )).getChannel() ) ) );
			try {
				String line;
				while( keepRun ) {
					line = stdin.readLine();

					if ( sessionListeners ) {
						Iterator it = sessionTokens.keySet().iterator();

						while( it.hasNext() ) {
							Value v = Value.create();
							v.getFirstChild( "token" ).setValue( it.next() );
							v.setValue( line );
							sendMessage( CommMessage.createRequest( "in", "/", v ) );
						}
					} else {
						sendMessage( CommMessage.createRequest( "in", "/", Value.create( line ) ) );
					}
				}
			} catch( ClosedByInterruptException ce ) {
			} catch( Exception e ) {
				e.printStackTrace();
			}
		}
	}
	
	private ConsoleInputThread consoleInputThread;

	@RequestResponse
	public void registerForInput( Value request )
	{

		if ( request.getFirstChild( "enableSessionListener").isDefined() ) {
			if ( request.getFirstChild( "enableSessionListener").boolValue() ) {
				sessionListeners = true;
				sessionTokens = new HashMap<String, String>();

			}
		}
		consoleInputThread = new ConsoleInputThread();
		consoleInputThread.start();
	}
	
	@Override
	protected void finalize()
	{
		consoleInputThread.kill();
	}

	@RequestResponse
	public void print( String s )
	{
		System.out.print( s );
	}

	@RequestResponse
	public void println( String s )
	{
		System.out.println( s );
	}

	@RequestResponse
	public void subscribeSessionListener( Value request )
	{
		String token = request.getFirstChild( "token" ).strValue();

		if ( sessionListeners ) {
			sessionTokens.put(  token, token );

		}
	}

	@RequestResponse
	public void unsubscribeSessionListener( Value request )
	{
		String token = request.getFirstChild( "token" ).strValue();

		if ( sessionListeners ) {
			sessionTokens.remove( token );

		}
	}
}
