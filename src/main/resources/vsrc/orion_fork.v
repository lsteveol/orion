`timescale 1ns/1ps

module orion_fork #(
  parameter P_INIT  = 0
)(
  input  wire       reset,
  
  input  wire       inA_req,
  output wire       inA_ack,
  
  output wire       outB_req,
  input  wire       outB_ack,
  output wire       outC_req,
  input  wire       outC_ack
  
);

reg  phase;
wire click;

assign outB_req = inA_req;
assign outC_req = inA_req;
assign inA_ack  = phase;

assign #10ps click = (outC_ack & outB_ack & ~phase) | (~outC_ack & ~outB_ack & phase);

always @(posedge click or posedge reset) begin
  if(reset) begin
    phase <= P_INIT;
  end else begin
    phase <= #10ps ~phase;
  end
end

endmodule

