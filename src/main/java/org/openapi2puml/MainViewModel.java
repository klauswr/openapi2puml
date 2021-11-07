package org.openapi2puml;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.openapi2puml.openapi.OpenApi2PlantUML;
import org.zkoss.bind.BindContext;
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.image.AImage;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zul.Filedownload;

import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Data
@Log4j2
public class MainViewModel {

	private SessionData sessionData;

	@Init
	public void init() {
		this.sessionData = (SessionData) Executions.getCurrent()
				.getAttribute("sessionData");
		if (this.sessionData == null) {
			sessionData = new SessionData();
			Executions.getCurrent()
					.setAttribute("sessionData", this.sessionData);
		}
	}

	public AImage getPlantUml() throws IOException {
		AImage aImage = new AImage(new URL("https://www.generationsforpeace.org/wp-content/uploads/2018/03/empty.jpg"));

		if (null != sessionData && null != sessionData.getTempDir()) {
			aImage = new AImage(sessionData.getTempDir()
					.toString() + "/swagger.png");
		}
		return aImage;
	}

	public AImage getImageContent() throws IOException {
		AImage aImage = new AImage(new URL("https://www.generationsforpeace.org/wp-content/uploads/2018/03/empty.jpg"));

		if (null != sessionData && null != sessionData.getTempDir()) {
			aImage = new AImage(sessionData.getTempDir()
					.toString() + "/swagger.png");
		}
		return aImage;
	}

	public AImage getSvgContent() throws IOException {
		AImage aImage = new AImage(new URL("https://www.generationsforpeace.org/wp-content/uploads/2018/03/empty.jpg"));

		if (null != sessionData && null != sessionData.getTempDir()) {
			aImage = new AImage(sessionData.getTempDir()
					.toString() + "/swagger.svg");
		}
		return aImage;
	}

	@Command("onUpload")
	@NotifyChange("*")
	public void onUploadF(BindContext ctx) throws IOException {

		UploadEvent upEvent = null;
		Object objUploadEvent = ctx.getTriggerEvent();
		if (objUploadEvent != null && (objUploadEvent instanceof UploadEvent)) {
			upEvent = (UploadEvent) objUploadEvent;
		}
		if (upEvent != null) {
			Media media = upEvent.getMedia();
			log.info("File Sucessfully uploaded [{}]", media.getName());

			Path tempFile = Files.createTempFile("openapi2puml", ".json");
			InputStream in = media.getStreamData();
			try (FileOutputStream out = new FileOutputStream(tempFile.toFile())) {
				IOUtils.copy(in, out);
			}
			log.info("File Sucessfully stored [{}]", tempFile.toString());

			Path tempDir = Files.createTempDirectory("openapi2puml");
			sessionData.setTempDir(tempDir);
			log.info("Target directory is [{}]", tempDir.toString());

			OpenApi2PlantUML.process(tempFile.toString(), tempDir.toString(), false, true, true, true);

//			 Messagebox.show("File Sucessfully processed. See "+ tempDir.toString());

		}
	}

	@Command("download")
	public void download(@BindingParam("type") String type) throws FileNotFoundException {
		Path tempDir = sessionData.getTempDir();
		Filedownload.save(tempDir.toString() + "/swagger." + type, null);
	}

	@Command("refresh")
	@NotifyChange("*")
	public void refresh() {

	}

}
