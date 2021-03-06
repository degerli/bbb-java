/*
 * GT-Mconf: Multiconference system for interoperable web and mobile
 * http://www.inf.ufrgs.br/prav/gtmconf
 * PRAV Labs - UFRGS
 * 
 * This file is part of Mconf-Mobile.
 *
 * Mconf-Mobile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Mconf-Mobile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Mconf-Mobile.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mconf.bbb.video;

import org.mconf.bbb.BigBlueButtonClient;
import org.mconf.bbb.users.Participant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flazr.rtmp.client.ClientOptions;
import com.flazr.rtmp.message.Video;
import com.flazr.util.Utils;

public class BbbVideoReceiver {

	protected class VideoConnection extends VideoReceiverConnection {

		public VideoConnection(ClientOptions options,
				BigBlueButtonClient context) {
			super(options, context);
		}

		@Override
		protected void onVideo(Video video) {
			BbbVideoReceiver.this.onVideo(video);
		}
		
	}
	
	private static final Logger log = LoggerFactory.getLogger(BbbVideoReceiver.class);

	private int userId;
	private String streamName;
	private VideoConnection videoConnection;
	
	public BbbVideoReceiver(int userId, BigBlueButtonClient context) {
		this.userId = userId;

		ClientOptions opt = new ClientOptions();
		opt.setClientVersionToUse(Utils.fromHex("00000000"));
		opt.setHost(context.getJoinService().getServerUrl().toLowerCase().replace("http://", ""));
		opt.setAppName("video/" + context.getJoinService().getJoinedMeeting().getConference());
		
		streamName = null;
		for (Participant p : context.getParticipants()) {
			if (p.getUserId() == userId && p.hasStream()) {
				streamName = p.getStatus().getStreamName();
				break;
			}
		}
		
		if (streamName == null) {
			log.debug("The userId = {} have no stream", userId);
			return;
		}
		
		opt.setWriterToSave(null);
		opt.setStreamName(streamName);
		
		videoConnection = new VideoConnection(opt, context);
	}
	
	protected void onVideo(Video video) {
		log.debug("received video package: {}", video.getHeader().getTime());
	}
	
	public void start() {
		if (videoConnection != null)
			videoConnection.connect();
	}
	
	public void stop() {
		if (videoConnection != null)
			videoConnection.disconnect();
	}
	
	public int getUserId() {
		return userId;
	}
	
	public String getStreamName() {
		return streamName;
	}
	
	public float getAspectRatio() {
		return getAspectRatio(userId, streamName);
	}
	
	public static float getAspectRatio(int userId, String streamName) {
		String userIdStr = Integer.toString(userId);
		if (streamName != null && streamName.contains(userIdStr)) {
			/*
			 * streamName is in this form: 160x1201-13173
			 * the timestamp doesn't interest, so we drop it
			 */
			if (streamName.matches("\\d+[x]\\d+[-]\\d+")) {
				streamName = streamName.substring(0, streamName.indexOf("-"));
			}
			/*
			 * streamName is in this form: 160x1201
			 * remove the userId and get the dimensions
			 */
			if (streamName.matches("\\d+[x]\\d+")) {
				String resStr = streamName.substring(0, streamName.lastIndexOf(userIdStr));
				String[] res = resStr.split("x");
				int width = Integer.parseInt(res[0]), 
					height = Integer.parseInt(res[1]);
				return width / (float) height;
			}
		}		
		return -1;
	}
}
