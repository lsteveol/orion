`timescale 1ns/1ps

module orion_join #(
  parameter P_INIT    = 0
)(
  input  wire       reset,
  
  input  wire       inA_req,
  output wire       inA_ack,
  input  wire       inB_req,
  output wire       inB_ack,
  
  output wire       outC_req,
  input  wire       outC_ack  
);

reg   phase;
//wire  click;
//assign #10ps click    = (inA_req & inB_req & ~phase) | (~inA_req & ~inB_req & phase);

reg click;
always @(*) begin
   click <= #10ps (inA_req & inB_req & ~phase) | (~inA_req & ~inB_req & phase);
end

assign outC_req = phase;

assign inA_ack  = outC_ack;
assign inB_ack  = outC_ack;

always @(posedge click or posedge reset) begin
  if(reset) begin
    phase <= P_INIT;
  end else begin
    $display("seen click in join %t", $realtime);
    phase <= #10ps ~phase;
  end
end


endmodule
