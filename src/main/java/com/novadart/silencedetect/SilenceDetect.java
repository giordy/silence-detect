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

import java.io.IOException;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class SilenceDetect {

	private static final Options OPTIONS = new Options();

	static {
		OPTIONS.addOption("h", false, "Print help");
		OPTIONS.addOption("i", true, "Input video file");
		OPTIONS.addOption("d", false, "Show debug output");
		OPTIONS.addOption("b", true, "Silence decibels threshold");
		OPTIONS.addOption("t", false, "Print audio tracks");
		OPTIONS.addOption("j", false, "Display Java Dialog");
		OPTIONS.addOption("s", true, "Extract audio track number <arg> from input video file");
	}


	public static void main(String[] args) {

		// create the parser
		CommandLineParser parser = new BasicParser();
		try {
			// parse the command line arguments
			CommandLine line = parser.parse( OPTIONS, args );

			if(line.hasOption("h")){

				printHelp();

			} else {

				String decibels = "-10";
				if(line.hasOption("b")){
					decibels = "-"+line.getOptionValue("b");
				} else {
					throw new RuntimeException();
				}
				

				String videoFile = null;

				if(line.hasOption("i")) {

					videoFile = line.getOptionValue("i");
					
					Boolean debug = line.hasOption("d");

					if(line.hasOption("t")) {

						System.out.println(printAudioTracksList(videoFile, debug));

					} else if(line.hasOption("s")) {

						int trackNumber = Integer.parseInt(line.getOptionValue("s"));
						System.out.println(printAudioTrackSilenceDuration(videoFile, trackNumber, decibels, debug));

					} else {

						printHelp();

					}


				} else if(line.hasOption("-j")){

					
					// choose file
					final JFileChooser fc = new JFileChooser();
					fc.setVisible(true);
					int returnVal = fc.showOpenDialog(null);
					if(returnVal == JFileChooser.APPROVE_OPTION) {

						videoFile = fc.getSelectedFile().getAbsolutePath();

					} else {
						return;
					}
					
					
					
					JTextArea tracks = new JTextArea();
					tracks.setText(printAudioTracksList(videoFile, true));
					JSpinner trackNumber = new JSpinner();
					trackNumber.setValue(1);
					final JComponent[] inputs = new JComponent[] {
							new JLabel("Audio Tracks"),
							tracks,
							new JLabel("Track to analyze"),
							trackNumber
					};
					JOptionPane.showMessageDialog(null, inputs, "Select Audio Track", JOptionPane.PLAIN_MESSAGE);
					
					
					JTextArea results = new JTextArea();
					results.setText(printAudioTrackSilenceDuration(videoFile, (int) trackNumber.getValue(), decibels, false));
					final JComponent[] resultsInputs = new JComponent[] {
							new JLabel("Results"),
							results
					};
					JOptionPane.showMessageDialog(null, resultsInputs, "RESULTS!", JOptionPane.PLAIN_MESSAGE);


				} else {
					printHelp();
					return;
				}

			}

		}
		catch( ParseException | IOException | InterruptedException exp ) {
			// oops, something went wrong
			System.out.println( "There was a problem :(\nReason: " + exp.getMessage() );
		}
	}

	private static void printHelp(){
		HelpFormatter formatter = new HelpFormatter();
		System.out.println("\n\nExamples:");
		System.out.println("---------");
		System.out.println("silence-detect -i videofile.mov -t");
		System.out.println("silence-detect -i videofile.mov -s 1");
		System.out.println("\n\n");
		formatter.printHelp("silence-detect", OPTIONS);
	}


	private static String printAudioTracksList(String videoFile, boolean debug) throws IOException{
		List<String> audioTracks = FfmpegUtils.getAudioTracks(videoFile, debug);
		StringBuilder sb = new StringBuilder();

		sb.append("\n");
		for (int i=0; i<audioTracks.size(); i++) {
			sb.append("[");
			sb.append(i);
			sb.append("] - ");
			sb.append(audioTracks.get(i));
			sb.append("\n");
		}
		sb.append("\n");
		return sb.toString();
	}


	private static String printAudioTrackSilenceDuration(String videoFile, int trackNumber, String decibels, boolean debug) throws IOException, InterruptedException{
		List<String> audioTracks = FfmpegUtils.getAudioTracks(videoFile, debug);
		if(trackNumber > audioTracks.size()-1){
			System.out.println("Invalid parameters!");
			return "Invalid parameters!";
		}

		SilenceStats ss = FfmpegUtils.calculateSilence(videoFile, trackNumber, decibels, debug);
		if(ss == null){
			System.out.println("Problems in analyzing file");
			return "Problems in analyzing file";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		sb.append("Track total duration: "+ss.getTotalDuration());
		sb.append("\n");
		sb.append("Silence duration (seconds): "+ss.getSilenceDuration() +" seconds which corresponds to " + ss.getSilenceDurationHumanReadable());
		
		return sb.toString();
	}	
}
