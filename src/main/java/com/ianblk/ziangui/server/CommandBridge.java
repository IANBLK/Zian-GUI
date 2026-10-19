package com.ianblk.ziangui.server;
import com.ianblk.ziangui.ZianGui;
import net.minecraft.server.level.ServerPlayer;
public final class CommandBridge {
 public enum Result {
  DISPATCHED(""), DENIED("No tienes acceso a esta función. Consulta los requisitos de tu rango."),
  MISSING("Este destino no está disponible. Informa al administrador."),
  UNAVAILABLE("El servicio de comandos no está disponible. Inténtalo cuando se restablezca."),
  UNCONFIRMED("No pudimos confirmar la acción. No la repitas si pudo consumir objetos o monedas; consulta al administrador.");
  public final String message;Result(String message){this.message=message;}
 }
 private CommandBridge(){}
 public static boolean execute(ServerPlayer p,String command){return executeDetailed(p,command)==Result.DISPATCHED;}
 public static Result executeDetailed(ServerPlayer p,String command){
  Object sender,target;java.lang.reflect.Method dispatch;
  try{
   sender=PermissionService.bukkitPlayer(p);
   if(sender==null)return vanilla(p,command);
   Class<?> bukkit=Class.forName("org.bukkit.Bukkit"),senderType=Class.forName("org.bukkit.command.CommandSender");
   Object server=bukkit.getMethod("getServer").invoke(null);
   Object map=server.getClass().getMethod("getCommandMap").invoke(server);
   target=Class.forName("org.bukkit.command.CommandMap").getMethod("getCommand",String.class).invoke(map,command.split(" ",2)[0]);
   if(target==null)return vanilla(p,command);
   if(!(Boolean)Class.forName("org.bukkit.command.Command").getMethod("testPermissionSilent",senderType).invoke(target,sender))return Result.DENIED;
   dispatch=bukkit.getMethod("dispatchCommand",senderType,String.class);
  }catch(ReflectiveOperationException|RuntimeException|LinkageError error){
   ZianGui.LOGGER.warn("[ZianGUI] Command lookup failed; no command executed",error);return Result.UNAVAILABLE;
  }
  try{return (Boolean)dispatch.invoke(null,sender,command)?Result.DISPATCHED:Result.UNCONFIRMED;}
  catch(ReflectiveOperationException|RuntimeException|LinkageError error){
   ZianGui.LOGGER.warn("[ZianGUI] Dispatch uncertain; no automatic retry",error);return Result.UNCONFIRMED;
  }
 }
 private static Result vanilla(ServerPlayer p,String command){
  var server=p.getServer();if(server==null)return Result.UNAVAILABLE;
  var dispatcher=server.getCommands().getDispatcher();var root=dispatcher.getRoot().getChild(command.split(" ",2)[0]);
  if(root==null)return Result.MISSING;
  if(!root.canUse(p.createCommandSourceStack()))return Result.DENIED;
  try{return dispatcher.execute(command,p.createCommandSourceStack())>0?Result.DISPATCHED:Result.UNCONFIRMED;}
  catch(Exception error){ZianGui.LOGGER.warn("[ZianGUI] Native dispatch uncertain; no automatic retry",error);return Result.UNCONFIRMED;}
 }
}
