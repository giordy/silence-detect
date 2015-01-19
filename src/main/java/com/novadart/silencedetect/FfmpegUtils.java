/*
 * Copyright (C) 2014 Giordano Battilana, Novadart
 *
 * This file is part of Silence-Detect.
 *
 * Silence-Detect is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Silence-Detect is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Silence-Detect.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.novadart.silencedetect;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FfmpegUtils {

	private static String CWD = System.getProperty("user.dir");
	private static String FFMPEG = CWD+"/ffmpeg";

//	static {
//
//		try {
//
//			FFMPEG = init();
//
//			Runtime.getRuntime().addShutdownHook(new Thread(){
//
//				@Override
//				public void run() {
//					if(FFMPEG != null){
//						new File(FFMPEG).delete();
//					}
//				}
//
//			});
//
//		} catch (IOException e) {
//			e.printStackTrace();
//			throw new RuntimeException();
//		}
//
//	}
//
//	private static String init() throws IOException{
//		File ffmpegTmp = File.createTempFile("_FFMPEG_", "");
//		if(ffmpegTmp.exists() && ffmpegTmp.canExecute()){
//			throw new IOException("FFMPEG not present!");
//		}
//
//		if(ffmpegTmp.exists()){
//			ffmpegTmp.delete();
//		}
//
//		try(
//				InputStream ffmpegFile = ClassLoader.class.getResourceAsStream("/ffmpeg");
//				FileOutputStream fos = new FileOutputStream(ffmpegTmp)
//				){
//
//			byte[] buffer = new byte[4096];
//
//			while(ffmpegFile.read(buffer) > -1){
//				fos.write(buffer);
//			}
//		}
//
//		ffmpegTmp.setExecutable(true);
//
//		return ffmpegTmp.getAbsolutePath();
//	}


	private static File getFile(String filePath, boolean debug) throws IOException{
		File f = new File(filePath);
		
		if(debug){ System.out.println("getFile: "+f.getAbsolutePath()); }
		
		f = f.isAbsolute() ? f : new File(CWD, filePath);
		
		if(debug){ System.out.println("getFile - computed: "+f.getAbsolutePath()); }
		
		if(!f.isFile()){
			throw new IOException("The file "+f.getAbsolutePath()+" does not exist!");
		}
		return f;
	}


	public static List<String> getAudioTracks(String videoFile, boolean debug) throws IOException{
		if(debug){ System.out.println("getAudioTracks"); }
		
		File video = getFile(videoFile, debug);

		try {
			// Execute command
			ProcessBuilder pb = new ProcessBuilder(Arrays.asList(new String[]{
					FFMPEG,
					"-i",
					video.getAbsolutePath()
			}));
			
			Process child = pb.start();

			List<String> audioTracks = new ArrayList<>();

			// Get output stream to write from it
			try(BufferedReader reader = 
					new BufferedReader(new InputStreamReader(child.getErrorStream()));){

				String line = null;
				while ( (line = reader.readLine()) != null ){
					if(debug){
						System.out.println(line);
					}
					if(line.contains("Audio")){
						audioTracks.add(line.trim());
					}
				}

				return audioTracks;
			}

		} catch (IOException e) {
			return new ArrayList<>();
		}

	}



	public static SilenceStats calculateSilence(String videoFile, int val, String decibels, boolean debug) throws IOException, InterruptedException{

		File outputTrack = new File(CWD, "audio"+val+".mp3");
		if(outputTrack.exists()){
			outputTrack.delete();
		}

		if(extractAudioTrack(videoFile, val, outputTrack, debug)){
			
			SilenceStats ss = new SilenceStats();
			ss.setSilenceDuration( extractAudioTrackTotalSilence(outputTrack, decibels, debug) );
			ss.setTotalDuration( extractAudioTrackDuration(outputTrack, debug) );
			return ss;

		} else {

		}

		return null;
	}


	private static boolean extractAudioTrack(String videoFile, int val, File outputTrack, boolean debug) throws IOException, InterruptedException{
		File video = getFile(videoFile, debug);

		try {
			// Execute command
			ProcessBuilder pb = new ProcessBuilder(Arrays.asList(new String[]{
					FFMPEG,
					"-i",
					video.getAbsolutePath(),
					"-map",
					"0:"+val,
					outputTrack.getAbsolutePath()
					}));
			Process child = pb.start();
			int exitVal = child.waitFor();
			return exitVal == 0;

		} catch (IOException e) {
			return false;
		}

	}


	private static String extractAudioTrackDuration(File outputTrack, boolean debug) throws IOException, InterruptedException{

		// Execute command
		ProcessBuilder pb = new ProcessBuilder(Arrays.asList(new String[]{
				FFMPEG,
				"-i",
				outputTrack.getAbsolutePath()
		}));
		Process child = pb.start();

		String duration = null;
		// Get output stream to write from it
		try(BufferedReader reader = 
				new BufferedReader(new InputStreamReader(child.getErrorStream()))){

			String line = null;
			while ( (line = reader.readLine()) != null ){
				if(debug){
					System.out.println(line);
				}
				if(line.contains("Duration:")){
					String[] toks = line.trim().split(" ");
					duration = toks[1].substring(0, toks[1].length()-1);
					break;
				}
			}
		}
		return duration==null ? "" : duration;
	}
	
	
	private static Double extractAudioTrackTotalSilence(File outputTrack, String decibels, boolean debug) throws IOException, InterruptedException{

		// Execute command
		ProcessBuilder pb = new ProcessBuilder(Arrays.asList(new String[]{
				FFMPEG,
				"-i",
				outputTrack.getAbsolutePath(),
				"-af",
				"silencedetect=n="+decibels+"dB:d=5",
				"-f",
				"null",
				"-"
				}));
		Process child = pb.start();

		// Get output stream to write from it
		Double total = 0d;
		
		try(BufferedReader reader = 
				new BufferedReader(new InputStreamReader(child.getErrorStream()))){

			String line = null;
			Double val;
			while ( (line = reader.readLine()) != null ){
				if(debug){
					System.out.println(line);
				}
				if(line.contains("silence_duration:")){
					String[] toks = line.trim().split(" ");
					val = Double.parseDouble(toks[ toks.length-1 ]);
					
					total = total + val;
				}
			}
		}
		
		return total;
	}

}
