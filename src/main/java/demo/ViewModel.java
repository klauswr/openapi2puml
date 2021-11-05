package demo;

import org.zkoss.bind.BindContext;
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.GlobalCommand;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;

import lombok.Data;

@Data
public class ViewModel {
	private String include="~./1.zul";
	private String hello;
	private boolean popup = false;
	
	@Init
	public void init() {
		hello = "Hello, demo";
	}
	
	@Command("reset")
	@NotifyChange("hello")
	public void reset() {
		hello = "Hello, reset";
	}
	
	@Command("popup")
	@NotifyChange("popup")
	public void popup() { 
		this.popup= true;
	}
	@Command("close")
	@NotifyChange("popup")
	public void close(BindContext ctx) { 
		ctx.getTriggerEvent().stopPropagation();
		this.popup= false;
	}

	
	@GlobalCommand("navigate")
	@NotifyChange("include")
	public void navigate(@BindingParam("to") String include) {
		setInclude(include);
	}
	
}
