package demo;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.openapi2puml.openapi.OpenApi2PlantUML;
import org.zkoss.bind.BindContext;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.image.AImage;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zul.Messagebox;

import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Data
@Log4j2
public class MainViewModel {
	private String img;
	private Path tempDir;

	public AImage getImageContent() throws IOException {
		AImage aImage;
		String image = this.img;
		if (StringUtils.isEmpty(img)) {
			aImage = new AImage(new URL("https://www.generationsforpeace.org/wp-content/uploads/2018/03/empty.jpg"));
		} else {
			aImage = new AImage(image);
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

			this.tempDir = Files.createTempDirectory("openapi2puml");
			log.info("Target directory is [{}]", tempDir.toString());

			OpenApi2PlantUML.process(tempFile.toString(), tempDir.toString(), false, true, true, true);

//			 Messagebox.show("File Sucessfully processed. See "+ tempDir.toString());

			this.img = this.tempDir.toString() + "/swagger.png";

		}
	}

}
